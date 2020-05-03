/*
Copyright IBM Corporation 2020. All Rights Reserved
SPDX-License-Identifier: Apache-2.0

Description : Unit Tests for Histogram-based percentile estimator
Author      : David Ohana (david.ohana@ibm.com)
*/

package com.ibm.hbpe

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldBeNear
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.rank.Percentile
import org.apache.commons.math3.stat.ranking.NaNStrategy
import org.apache.commons.math3.util.KthSelector
import org.apache.commons.math3.util.MedianOf3PivotingStrategy
import org.junit.Test
import java.util.*


class HbpeTest {
    @Test
    fun testFloorTo() {
        10.1.floorTo(0.1).shouldBeEqualTo(10.1)
        2.0.floorTo(1.0).shouldBeEqualTo(2.0)
        2.0.floorTo(10.0).shouldBeEqualTo(0.0)
        0.22.floorTo(1.0).shouldBeEqualTo(0.0)
        0.22.floorTo(0.1).shouldBeEqualTo(0.2)
        0.29.floorTo(0.1).shouldBeEqualTo(0.2)
        0.0.floorTo(0.1).shouldBeEqualTo(0.0)
    }

    @Test
    fun testFloorToNoPrecisionLoss() {
        val rnd = Random(2)
        for (i in 0..1000) {
            val v = rnd.nextDouble() * 0.1 + 15 + 0.1
            val f = v.floorTo(0.1)
            f.shouldBeEqualTo(15.1)
        }
    }

    @Test
    fun testAddValues() {
        val rnd = Random(1)
        for (i in 0..2) {
            val hbpe = HistogramBasedPercentileEstimator(0.1)
            hbpe.getRankThenAdd(0.0)
            for (j in 0..300) {
                val v = rnd.nextDouble() * 100000
                val r = hbpe.getRankThenAdd(v)
                r.shouldBeInRange(0.0, 100.0)
            }
        }
    }

    @Test
    fun testGetPercentile() {
        val samples = mutableListOf<Double>()

        val apacheImpl = PercentileInclusive()

        val bucketSize = 0.1
        val hbpe = HistogramBasedPercentileEstimator(bucketSize)

        fun assertResult(p: Double) {
            val apacheResult = apacheImpl.evaluate(samples.toDoubleArray(), p)
            val hbpeResult = hbpe.getPercentile(p)
            hbpeResult.shouldBeNear(apacheResult, bucketSize)
        }

        fun addAndAssert(v: Double) {
            samples.add(v)
            hbpe.addValue(v)
            assertResult(100.0)
            assertResult(99.9)
            assertResult(99.0)
            assertResult(95.0)
            assertResult(80.0)
            assertResult(75.0)
            assertResult(66.0)
            assertResult(50.0)
            assertResult(33.0)
            assertResult(25.0)
            assertResult(20.0)
            assertResult(10.0)
            assertResult(5.0)
            assertResult(1.0)
            assertResult(0.1)
        }

        addAndAssert(10.0)
        addAndAssert(20.0)
        addAndAssert(30.0)
        addAndAssert(40.0)
        addAndAssert(0.0)
        addAndAssert(50.0)
        addAndAssert(0.0)
        addAndAssert(100.0)
        addAndAssert(29.0)
        addAndAssert(99.0)
        addAndAssert(100.0)
        addAndAssert(1.0)
        addAndAssert(-1.0)
        addAndAssert(3.0)
        addAndAssert(3.0)
        addAndAssert(30.0)
        addAndAssert(100.0)
        addAndAssert(100.0)
        addAndAssert(1000.0)
        addAndAssert(-1000.0)
        addAndAssert(0.0)

        val rnd = Random(3)
        for (i in 1..1000) {
            val v = rnd.nextDouble() * 200 - 300
            addAndAssert(v)
        }

    }

    @Test
    fun testGetRank() {
        val rnd = Random(4)

        val values = mutableListOf<Double>()
        val bucketSize = 0.1
        val hbpe = HistogramBasedPercentileEstimator(bucketSize)

        fun addValue(v: Double) {
            values.add(v)
            hbpe.addValue(v)
        }
        addValue(10.0)

        fun assertAndAdd(v: Double) {
            val simplePr = calculateSimplePercentileRank(values, v)
            val hbpePr = hbpe.getRank(v)
            hbpePr.shouldBeNear(simplePr, bucketSize)
            addValue(v)
        }

        assertAndAdd(40.0)
        assertAndAdd(20.0)
        assertAndAdd(30.0)
        assertAndAdd(30.0)

        // the accuracy gets betters as the population size is higher, so we fill with many samples initially
        for (i in 1..100000) {
            val v = rnd.nextDouble() * 200 - 300
            addValue(v)
        }

        for (i in 1..1000) {
            val v = rnd.nextDouble() * 200 - 300
            assertAndAdd(v)
        }
    }

    /*
    Naive implementation of PR. Formula according to https://en.wikipedia.org/wiki/Percentile_rank
     */
    fun calculateSimplePercentileRank(values: List<Double>, rankOf: Double): Double {

        val countLess = values.count { it < rankOf }
        val countEqual = values.count { it == rankOf }
        return (countLess + 0.5 * countEqual) / values.size * 100
    }

    @Test
    fun testGetPercentilePerf() {
        val rnd = Random(5)
        val math3stats = DescriptiveStatistics()
        val hbpe = HistogramBasedPercentileEstimator(0.1)

        fun singleRunMath3(v: Double) {
            math3stats.addValue(v)
            math3stats.getPercentile(100.0)
            math3stats.getPercentile(99.0)
            math3stats.getPercentile(75.0)
            math3stats.getPercentile(50.0)
            math3stats.getPercentile(25.0)
            math3stats.getPercentile(1.0)
        }

        fun singleRunHbpe(v: Double) {
            hbpe.addValue(v)
            hbpe.getPercentile(100.0)
            hbpe.getPercentile(99.0)
            hbpe.getPercentile(75.0)
            hbpe.getPercentile(50.0)
            hbpe.getPercentile(25.0)
            hbpe.getPercentile(1.0)
        }

        fun bench(name: String, singleRun: (Double) -> Unit) {
            val start1 = System.currentTimeMillis()
            for (i in 1..10000) {
                val v = rnd.nextDouble() * 200 - 300
                singleRun(v)
            }
            val tookSec = (System.currentTimeMillis() - start1) / 1000.0
            println("$name took: $tookSec sec")
        }

        bench("Math3", ::singleRunMath3)
        math3stats.clear()
        bench("Math3", ::singleRunMath3)

        bench("Hbpe", ::singleRunHbpe)
        hbpe.clear()
        bench("Hbpe", ::singleRunHbpe)
    }
}

/*
 Excel style percentile.
 There are quite a few formulas for percentile calculation.
 This adjust the apache impl to ve consistent with the Excel formula (PERCENTILE.INC).
 https://en.wikipedia.org/wiki/Percentile#Second_variant,_'%22%60UNIQ--postMath-00000047-QINU%60%22'
 */
class PercentileInclusive : Percentile(
    50.0,
    EstimationType.R_7,  // use excel style interpolation
    NaNStrategy.REMOVED,
    KthSelector(MedianOf3PivotingStrategy())
)