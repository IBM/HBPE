package com.ibm.hbpe;

public final class ChildrenBmiCalculator {
    public static void main(String[] args) {

        HistogramBasedPercentileEstimator hbpe = new HistogramBasedPercentileEstimator(1);

        System.out.println("Populating with synthetic data ..");
        for (int i = 0; i < 10000000; i++) {
            double height = Math.random() * 150 + 50;
            hbpe.addValue(height);
        }

        System.out.printf("Median is %s cm%n", hbpe.getPercentile(50.0));
        System.out.printf("Rank of height 198.0cm is %s%n", hbpe.getPercentileRank(198));
    }
}
