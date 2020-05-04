package com.ibm.hbpe;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.Random;

public final class BenchmarkGetPercentile {
    static int n = 30000;
    static int percentile1 = 75;
    static int percentile2 = 10;

    public static void main(String[] args) {
        benchmarkNaive();
        benchmarkApacheMath3();
        benchmarkHbpe();
    }

    private static void benchmarkApacheMath3() {
        System.out.println("*** Starting Apache Math3 benchmark");
        Random rnd = new Random(10);
        DescriptiveStatistics ds = new DescriptiveStatistics();
        long startTimeMs = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            double v = rnd.nextDouble() * 200 - 100;
            ds.addValue(v);
            double p1 = ds.getPercentile(percentile1);
            double p2 = ds.getPercentile(percentile2);
        }
        long stopTimeMs = System.currentTimeMillis();
        double tookSec = (stopTimeMs - startTimeMs) / 1000.0;
        System.out.printf("Apache math3 took %s ms%n", tookSec);
        System.out.printf("final %s percentile is %s%n", percentile1, ds.getPercentile(percentile1));
        System.out.printf("final %s percentile is %s%n", percentile2, ds.getPercentile(percentile2));
        System.out.println();
    }

    private static void benchmarkHbpe() {
        System.out.println("*** Starting HBPE benchmark");
        Random rnd = new Random(10);
        HistogramBasedPercentileEstimator hbpe = new HistogramBasedPercentileEstimator(1);
        long startTimeMs = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            double v = rnd.nextDouble() * 200 - 100;
            hbpe.addValue(v);
            double p1 = hbpe.getPercentile(percentile1);
            double p2 = hbpe.getPercentile(percentile2);
        }
        long stopTimeMs = System.currentTimeMillis();
        double tookSec = (stopTimeMs - startTimeMs) / 1000.0;
        System.out.printf("HBPE took %s ms%n", tookSec);
        System.out.printf("final %s percentile is %s%n", percentile1, hbpe.getPercentile(percentile1));
        System.out.printf("final %s percentile is %s%n", percentile2, hbpe.getPercentile(percentile2));
        System.out.println();
    }


    private static void benchmarkNaive() {
        System.out.println("*** Starting Naive benchmark");
        ArrayList<Double> population = new ArrayList<>();
        Random rnd = new Random(10);

        long startTimeMs = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            double v = rnd.nextDouble() * 200 - 100;
            population.add(v);
            population.sort(Double::compareTo);
            double p1 = population.get(population.size() * percentile1 / 100);
            double p2 = population.get(population.size() * percentile2 / 100);
        }
        long stopTimeMs = System.currentTimeMillis();
        double tookSec = (stopTimeMs - startTimeMs) / 1000.0;
        System.out.printf("Naive took %s ms%n", tookSec);
        System.out.printf("final %s percentile is %s%n", percentile1, population.get(population.size() * percentile1 / 100));
        System.out.printf("final %s percentile is %s%n", percentile2, population.get(population.size() * percentile2 / 100));
        System.out.println();
    }
}
