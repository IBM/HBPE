package hbpeDemo;

import com.ibm.hbpe.HistogramBasedPercentileEstimator;

public final class SimpleUsage {
    public static void main(String[] args) {
        HistogramBasedPercentileEstimator hbpe = new HistogramBasedPercentileEstimator();

        hbpe.addValue(30.0);
        hbpe.addValue(40.0);
        hbpe.addValue(20.0);
        hbpe.addValue(10.0);

        System.out.println("p50: " + hbpe.getPercentile(50.0));
        System.out.println("p75: " + hbpe.getPercentile(75.0));
        System.out.println("p25: " + hbpe.getPercentile(25.0));

        System.out.println("PR of 38: " + hbpe.getPercentileRank(38.0));
        System.out.println("PR of 25: " + hbpe.getPercentileRank(25.0));
    }
}


