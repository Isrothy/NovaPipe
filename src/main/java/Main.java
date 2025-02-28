import Producer.BinanceUs.DataProducer;
import Producer.BinanceUs.MarketDataStreamType;

public class Main {
    public static void main(String[] args) throws Exception {
        // For testing purposes, we'll use "btcusdt" as our symbol.
        String symbol = "btcusdt";

        // Create a thread for each stream type.
        Thread tradeThread = new Thread(() -> {
            try {
                System.out.println("Starting TradeData stream");
                DataProducer producer = new DataProducer(symbol, new MarketDataStreamType.TradeData());
                producer.produceRawExchangeData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread aggTradeThread = new Thread(() -> {
            try {
                System.out.println("Starting AggregateTrade stream");
                DataProducer producer = new DataProducer(symbol, new MarketDataStreamType.AggregateTrade());
                producer.produceRawExchangeData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread klineThread = new Thread(() -> {
            try {
                System.out.println("Starting CandlestickData stream (ONE_MIN)");
                DataProducer producer = new DataProducer(symbol,
                        new MarketDataStreamType.CandlestickData(MarketDataStreamType.CandlestickChartInterval.ONE_MIN));
                producer.produceRawExchangeData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread tickerThread = new Thread(() -> {
            try {
                System.out.println("Starting TickerOrder stream");
                DataProducer producer = new DataProducer(symbol, new MarketDataStreamType.TickerOrder());
                producer.produceRawExchangeData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread partialThread = new Thread(() -> {
            try {
                System.out.println("Starting PartialOrderBookDepth stream (LEVEL_5, MS_100)");
                DataProducer producer = new DataProducer(symbol,
                        new MarketDataStreamType.PartialOrderBookDepth(
                                MarketDataStreamType.DepthLevel.LEVEL_5,
                                MarketDataStreamType.UpdateSpeed.MS_100));
                producer.produceRawExchangeData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread diffThread = new Thread(() -> {
            try {
                System.out.println("Starting OrderBookDepthDiff stream (MS_1000)");
                DataProducer producer = new DataProducer(symbol,
                        new MarketDataStreamType.OrderBookDepthDiff(MarketDataStreamType.UpdateSpeed.MS_1000));
                producer.produceRawExchangeData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread customThread = new Thread(() -> {
            try {
                System.out.println("Starting Custom stream (customStream)");
                DataProducer producer = new DataProducer(symbol, new MarketDataStreamType.Custom("depth5@100ms"));
                producer.produceRawExchangeData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Start all threads.
//        tradeThread.start();
//        aggTradeThread.start();
//        klineThread.start();
//        tickerThread.start();
//        partialThread.start();
//        diffThread.start();
        customThread.start();

        // Join threads to keep the main method running (these threads block indefinitely
        // as each producer waits for incoming data).
//        tradeThread.join();
//        aggTradeThread.join();
//        klineThread.join();
//        tickerThread.join();
//        partialThread.join();
//        diffThread.join();
        customThread.join();
    }
}