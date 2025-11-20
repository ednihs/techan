package com.stockanalyzer.ml;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer;
import org.nd4j.linalg.factory.Nd4j;

import java.io.*;

public class LstmTradesModel implements Serializable {

    private final MultiLayerNetwork model;
    private final NormalizerMinMaxScaler tradeScaler;
    private final NormalizerMinMaxScaler volumeScaler;
    private final int windowSize;

    public LstmTradesModel(MultiLayerNetwork model, NormalizerMinMaxScaler tradeScaler, NormalizerMinMaxScaler volumeScaler, int windowSize) {
        this.model = model;
        this.tradeScaler = tradeScaler;
        this.volumeScaler = volumeScaler;
        this.windowSize = windowSize;
    }

    public void save(File file) throws IOException {
        // Save the neural network model with scalers using NormalizerSerializer
        model.save(file, true);
        
        // Save scaler statistics using NormalizerSerializer
        String basePath = file.getAbsolutePath();
        File tradeScalerFile = new File(basePath + ".trade_scaler");
        File volumeScalerFile = new File(basePath + ".volume_scaler");
        
        NormalizerSerializer.getDefault().write(tradeScaler, tradeScalerFile);
        NormalizerSerializer.getDefault().write(volumeScaler, volumeScalerFile);
    }

    public static LstmTradesModel load(File file, int windowSize) throws IOException {
        // Load the neural network model
        MultiLayerNetwork model = MultiLayerNetwork.load(file, true);
        
        // Load scaler statistics using NormalizerSerializer
        String basePath = file.getAbsolutePath();
        File tradeScalerFile = new File(basePath + ".trade_scaler");
        File volumeScalerFile = new File(basePath + ".volume_scaler");
        
        // Check if scaler files exist - they are required for predictions
        if (!tradeScalerFile.exists() || !volumeScalerFile.exists()) {
            throw new IOException("Scaler files not found for model. This model was trained with an older version. " +
                    "Please retrain the model. Missing files: " + 
                    (!tradeScalerFile.exists() ? tradeScalerFile.getName() + " " : "") +
                    (!volumeScalerFile.exists() ? volumeScalerFile.getName() : ""));
        }
        
        try {
            NormalizerMinMaxScaler tradeScaler = NormalizerSerializer.getDefault().restore(tradeScalerFile);
            NormalizerMinMaxScaler volumeScaler = NormalizerSerializer.getDefault().restore(volumeScalerFile);
            
            return new LstmTradesModel(model, tradeScaler, volumeScaler, windowSize);
        } catch (Exception e) {
            throw new IOException("Failed to load scaler files", e);
        }
    }

    public double predict(double[] tradeHistory, double[] volumeHistory) {
        if (tradeHistory.length != windowSize) {
            throw new IllegalArgumentException("Trade history must have length " + windowSize);
        }
        if (volumeHistory.length != windowSize + 1) {
            throw new IllegalArgumentException("Volume history must have length " + (windowSize + 1));
        }

        // Prepare input for prediction
        INDArray features = Nd4j.create(1, 3, windowSize);
        double currentVolume = volumeHistory[windowSize];

        for (int i = 0; i < windowSize; i++) {
            features.putScalar(new int[]{0, 0, i}, tradeHistory[i]);
            features.putScalar(new int[]{0, 1, i}, volumeHistory[i]);
            features.putScalar(new int[]{0, 2, i}, currentVolume);
        }

        // You must scale the prediction input data using the same scalers from training
        // This is a simplified approach; ideally, scalers would be saved with the model
        INDArray tradeFeatures = features.tensorAlongDimension(0, 1, 2).getColumns(0);
        INDArray volumeFeatures = features.tensorAlongDimension(0, 1, 2).getColumns(1);
        INDArray currentVolumeFeatures = features.tensorAlongDimension(0, 1, 2).getColumns(2);

        tradeScaler.transform(tradeFeatures);
        volumeScaler.transform(volumeFeatures);
        volumeScaler.transform(currentVolumeFeatures);

        INDArray output = model.output(features);
        
        // Extract the prediction from the last timestep
        // Output shape is [batchSize=1, numOutputs=1, timeSeriesLength=windowSize]
        // We want the value at [0, 0, windowSize-1]
        double scaledPrediction = output.getDouble(0, 0, windowSize - 1);
        
        // Revert scaling to get the actual predicted value
        // Manual revert: value = scaled * (max - min) + min
        INDArray min = tradeScaler.getMin();
        INDArray max = tradeScaler.getMax();
        double minVal = min.getDouble(0);
        double maxVal = max.getDouble(0);
        double actualPrediction = scaledPrediction * (maxVal - minVal) + minVal;

        return actualPrediction;
    }
}
