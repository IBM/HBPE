//package com.ibm.hbpe;
//
//import java.util.ArrayList;
//
//public final class LatencyAnomalyDetection {
//
//    static ArrayList<Integer> arr = new ArrayList<>();
//
//    public static long measureLatency() {
//        long start = System.nanoTime();
//        arr.add(333);
//        long end = System.nanoTime();
//        return end - start;
//    }
//
//    public static void main(String[] args) {
//
//        HistogramBasedPercentileEstimator hbpe = new HistogramBasedPercentileEstimator(1);
//
//        System.out.println("Measuring latencies");
//        for (int i = 0; i < 100000; i++) {
//            double latency = measureLatency();
//            double pr = hbpe.getRankThenAdd(latency);
//            if (pr >= 99.99) {
//                System.out.printf("Latency of %s ns is unusual (PR=%s)\n", latency, pr);
//            }
//        }
//
//        System.out.println("-------------------------------");
//        System.out.printf("p0 is %s ns%n", hbpe.getPercentile(0.0));
//        System.out.printf("p01 is %s ns%n", hbpe.getPercentile(1.0));
//        System.out.printf("p10 is %s ns%n", hbpe.getPercentile(10.0));
//        System.out.printf("p50 is %s ns%n", hbpe.getPercentile(50.0));
//        System.out.printf("p90 is %s ns%n", hbpe.getPercentile(90.0));
//        System.out.printf("p99 is %s ns%n", hbpe.getPercentile(99.0));
//        System.out.printf("p100 is %s ns%n", hbpe.getPercentile(100.0));
//    }
//}
