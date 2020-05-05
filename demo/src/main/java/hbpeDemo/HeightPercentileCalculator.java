package hbpeDemo;

import com.ibm.hbpe.HistogramBasedPercentileEstimator;

public final class HeightPercentileCalculator {
    public static void main(String[] args) {
        HistogramBasedPercentileEstimator hbpe = new HistogramBasedPercentileEstimator(1);

        System.out.println("Populating with synthetic data ..");
        for (int i = 0; i < 10000000; i++) {
            double heightCm = Math.random() * 150 + 50;
            hbpe.addValue(heightCm);
        }

        System.out.printf("Median is %s cm%n", hbpe.getPercentile(50.0));
        int myHeightCm = 190;
        double percentileRank = hbpe.getPercentileRank(myHeightCm);
        System.out.printf("Percentile Rank of height %s cm is %s%%%n", myHeightCm, percentileRank);
    }
}


