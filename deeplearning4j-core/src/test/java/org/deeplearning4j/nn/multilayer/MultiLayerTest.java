package org.deeplearning4j.nn.multilayer;

import org.apache.commons.math3.random.MersenneTwister;
import org.deeplearning4j.datasets.iterator.DataSetIterator;
import org.deeplearning4j.datasets.iterator.impl.IrisDataSetIterator;
import org.deeplearning4j.datasets.iterator.impl.LFWDataSetIterator;
import org.deeplearning4j.distributions.Distributions;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.models.featuredetectors.rbm.RBM;
import org.deeplearning4j.nn.api.LayerFactory;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.layers.OutputLayer;
import org.deeplearning4j.nn.layers.factory.DefaultLayerFactory;
import org.deeplearning4j.nn.layers.factory.LayerFactories;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.stepfunctions.GradientStepFunction;
import org.deeplearning4j.plot.iterationlistener.NeuralNetPlotterIterationListener;
import org.junit.Test;
import org.nd4j.linalg.api.activation.Activations;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by agibsonccc on 12/27/14.
 */
public class MultiLayerTest {

    private static final Logger log = LoggerFactory.getLogger(MultiLayerTest.class);

    @Test
    public void testDbnFaces() {
        Nd4j.dtype = DataBuffer.DOUBLE;
        LayerFactory layerFactory = LayerFactories.getFactory(RBM.class);
        DataSetIterator iter = new LFWDataSetIterator(28,28);


        DataSet next = iter.next();
        next.normalizeZeroMeanZeroUnitVariance();

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(OptimizationAlgorithm.CONJUGATE_GRADIENT)
                .constrainGradientToUnitNorm(true)
                .weightInit(WeightInit.DISTRIBUTION).dist(Distributions.normal(new MersenneTwister(123), 1e-5))
                .iterations(100).learningRate(1e-3).iterationListener(new NeuralNetPlotterIterationListener(10))
                .nIn(next.numInputs()).nOut(next.numOutcomes()).visibleUnit(RBM.VisibleUnit.GAUSSIAN).hiddenUnit(RBM.HiddenUnit.RECTIFIED).layerFactory(layerFactory)
                .list(4).hiddenLayerSizes(600,250,100).override(new NeuralNetConfiguration.ConfOverride() {
                    @Override
                    public void override(int i, NeuralNetConfiguration.Builder builder) {
                        if (i == 3) {
                            builder.layerFactory(new DefaultLayerFactory(OutputLayer.class));
                            builder.activationFunction(Activations.softMaxRows());
                            builder.lossFunction(LossFunctions.LossFunction.MCXENT);

                        }
                    }
                }).build();

        MultiLayerNetwork network = new MultiLayerNetwork(conf);
        network.fit(next);

    }

    @Test
    public void testDbn() {
        LayerFactory layerFactory = LayerFactories.getFactory(RBM.class);
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(OptimizationAlgorithm.CONJUGATE_GRADIENT)
                .iterations(100).weightInit(WeightInit.VI).stepFunction(new GradientStepFunction())
                .activationFunction(Activations.tanh())
                .nIn(4).nOut(3).visibleUnit(RBM.VisibleUnit.GAUSSIAN).hiddenUnit(RBM.HiddenUnit.RECTIFIED).layerFactory(layerFactory)
                .list(3).hiddenLayerSizes(new int[]{3, 2}).override(new NeuralNetConfiguration.ConfOverride() {
                    @Override
                    public void override(int i, NeuralNetConfiguration.Builder builder) {
                        if (i == 2) {
                            builder.layerFactory(new DefaultLayerFactory(OutputLayer.class));
                            builder.activationFunction(Activations.softMaxRows());
                            builder.lossFunction(LossFunctions.LossFunction.MCXENT);

                        }
                    }
                }).build();

        MultiLayerNetwork network = new MultiLayerNetwork(conf);
        DataSetIterator iter = new IrisDataSetIterator(150, 150);


        DataSet next = iter.next();
        next.normalizeZeroMeanZeroUnitVariance();
        SplitTestAndTrain trainTest = next.splitTestAndTrain(110);
        network.fit(trainTest.getTrain());


        DataSet test = trainTest.getTest();
        Evaluation eval = new Evaluation();
        INDArray output = network.output(test.getFeatureMatrix());
        eval.eval(test.getLabels(),output);
        log.info("Score " +eval.stats());


    }

}
