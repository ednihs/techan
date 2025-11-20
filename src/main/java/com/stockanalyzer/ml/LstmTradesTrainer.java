package com.stockanalyzer.ml;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;



import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LstmTradesTrainer {

    private static final int epochs = 50;
    private static final int lstmLayerSize = 50;
    private static final int denseLayerSize = 20;


    public static LstmTradesModel trainForSymbol(
            Connection conn, String symbol, int windowSize, double learningRate) throws SQLException {

        List<PriceDataRow> rows = loadPriceData(conn, symbol);

        // Hold out the last 10 records for prediction/validation
        if (rows.size() > 10) {
            rows = rows.subList(0, rows.size() - 10);
        }

        if (rows.size() < windowSize + 1) {
            throw new IllegalStateException("Not enough training rows for symbol " + symbol + " to train with windowSize=" + windowSize);
        }

        // --- Data Preparation & Scaling ---
        NormalizerMinMaxScaler tradeScaler = new NormalizerMinMaxScaler(0, 1);
        NormalizerMinMaxScaler volumeScaler = new NormalizerMinMaxScaler(0, 1);
        
        List<Double> tradeData = new ArrayList<>();
        List<Double> volumeData = new ArrayList<>();
        for(PriceDataRow row : rows) {
            tradeData.add((double)row.getNoOfTrades());
            volumeData.add((double)row.getVolume());
        }

        INDArray tradeArray = Nd4j.create(tradeData.stream().mapToDouble(d -> d).toArray(), new long[]{tradeData.size(), 1});
        INDArray volumeArray = Nd4j.create(volumeData.stream().mapToDouble(d -> d).toArray(), new long[]{volumeData.size(), 1});

        tradeScaler.fit(new DataSet(tradeArray, tradeArray));
        volumeScaler.fit(new DataSet(volumeArray, volumeArray));

        tradeScaler.transform(tradeArray);
        volumeScaler.transform(volumeArray);

        // --- Create Sliding Window Samples ---
        int numSamples = rows.size() - windowSize;
        INDArray features = Nd4j.create(numSamples, 3, windowSize);
        // Labels need to be rank 3 for RNN: [numSamples, numOutputs, timeSeriesLength]
        INDArray labels = Nd4j.create(numSamples, 1, windowSize);

        for (int i = 0; i < numSamples; i++) {
            for (int j = 0; j < windowSize; j++) {
                features.putScalar(new int[]{i, 0, j}, tradeArray.getDouble(i + j)); // Past Trades
                features.putScalar(new int[]{i, 1, j}, volumeArray.getDouble(i + j)); // Past Volumes
            }
            // Add current day's volume as a feature across all timesteps
            double currentVolume = volumeArray.getDouble(i + windowSize);
            for (int j = 0; j < windowSize; j++) {
                 features.putScalar(new int[]{i, 2, j}, currentVolume);
            }
            // For RNN output, we only care about the last timestep prediction
            // Set the label at the last timestep
            double targetTrades = tradeArray.getDouble(i + windowSize);
            labels.putScalar(new int[]{i, 0, windowSize - 1}, targetTrades);
        }

        DataSet dataSet = new DataSet(features, labels);

        // --- Network Configuration ---
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(123)
                .weightInit(WeightInit.XAVIER)
                .updater(new Adam(learningRate))
                .list()
                .layer(0, new LSTM.Builder().nIn(3).nOut(lstmLayerSize)
                        .activation(Activation.TANH).build())
                .layer(1, new DenseLayer.Builder().nIn(lstmLayerSize).nOut(denseLayerSize)
                        .activation(Activation.RELU).build())
                .layer(2, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY).nIn(denseLayerSize).nOut(1).build())
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        model.setListeners(new ScoreIterationListener(10));

        // --- Training ---
        for (int i = 0; i < epochs; i++) {
            model.fit(dataSet);
        }

        return new LstmTradesModel(model, tradeScaler, volumeScaler, windowSize);
    }
    
    private static List<PriceDataRow> loadPriceData(Connection conn, String symbol) throws SQLException {
        // This is the same as the previous implementation
        String sql = "SELECT trade_date, symbol, volume, no_of_trades FROM price_data WHERE symbol = ? AND volume IS NOT NULL AND no_of_trades IS NOT NULL ORDER BY trade_date ASC";
        List<PriceDataRow> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new PriceDataRow(
                            rs.getDate("trade_date").toLocalDate(),
                            rs.getString("symbol"),
                            rs.getLong("volume"),
                            rs.getInt("no_of_trades")
                    ));
                }
            }
        }
        return rows;
    }
}
