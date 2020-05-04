/*
Copyright IBM Corporation 2020. All Rights Reserved
SPDX-License-Identifier: Apache-2.0

Description : Implementation of an Histogram-based percentile estimator
Author      : David Ohana (david.ohana@ibm.com)
*/

@file:Suppress("UnnecessaryVariable")

package com.ibm.hbpe

import org.apache.commons.math3.util.Precision
import java.math.RoundingMode
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt


/**
 * Creates a new Histogram-based Percentile Estimator
 *
 * The estimator inserts the population into buckets of a predefined size instead of storing the whole population.
 * This is beneficial in case the population size is significantly higher than the value range.
 *
 * The precision scale affects the precision of the estimator, as a trade-of of required memory / run-time.
 * Both required memory and calculation time is O(number of buckets), which is [getValueRange] / [bucketSize].
 *
 * @param precisionScale the required resolution, which affect the bucket size of the histogram. Bucket size is
 * `1/10^precisionScale` (`0` for bucket size `1.0`, `1` for bucket size `0.1`, `2` for bucket size `0.01` and so on)
 */
class HistogramBasedPercentileEstimator(val precisionScale: Int = 1) {
    val bucketSize = 10.0.pow(-precisionScale)

    internal val bucketsValueCount = mutableListOf<Int>()
    internal val bucketsHighBound = mutableListOf<Double>()

    var valueCount = 0
        private set

    var lowBoundInclusive = Double.NaN
        private set

    var higBoundExclusive = Double.NaN
        private set

    /**
     * This constructor is intended for creating an instance out of a serialized data
     */
    constructor(
        precisionScale: Int,
        lowBoundInclusive: Double,
        bucketsValueCount: List<Int>
    ) : this(precisionScale) {
        this.bucketsValueCount.addAll(bucketsValueCount)
        this.lowBoundInclusive = lowBoundInclusive
        this.valueCount = bucketsValueCount.sum()

        var higBoundExclusive = lowBoundInclusive
        for (bucketIndex in bucketsValueCount.indices) {
            higBoundExclusive += bucketSize
            higBoundExclusive = higBoundExclusive.roundResolution(precisionScale)
            bucketsHighBound.add(higBoundExclusive)
        }

        this.higBoundExclusive = higBoundExclusive
    }

    /**
     * Copy constructor
     */
    constructor(src: HistogramBasedPercentileEstimator) : this(
        src.precisionScale,
        src.lowBoundInclusive,
        src.bucketsValueCount
    )

    /**
     * Reset the state of the instance
     *
     */
    fun clear() {
        valueCount = 0
        lowBoundInclusive = Double.NaN
        higBoundExclusive = Double.NaN
        bucketsValueCount.clear()
        bucketsHighBound.clear()
    }

    /**
     * Calculate the range of the population
     * @return High bound - low bound, or [Double.NaN] is population is empty.
     */
    fun getValueRange(): Double {
        if (valueCount == 0)
            return Double.NaN
        val range = higBoundExclusive - lowBoundInclusive
        return range.roundResolution(precisionScale)
    }

    /**
     * Calculate the `p` th percentile of the current population.
     *
     * @param p The required percentile in range 0 .. 100
     * @return Estimated value in the requested percentile or [Double.NaN] is population is empty.
     */
    fun getPercentile(p: Double): Double {
        //  possible optimization: traverse in reverse if p > 50

        require(p.isFinite())
        require(p >= 0)
        require(p <= 100)

        if (valueCount == 0)
            return Double.NaN

        if (p == 0.0)
            return lowBoundInclusive

        if (p == 100.0)
            return higBoundExclusive

        val targetPos = p / 100 * (valueCount - 1) + 1
        var countSeen = 0.0
        var curBucketHig = lowBoundInclusive
        var prvBucketLow = lowBoundInclusive

        for (bucketIndex in 0 until bucketsValueCount.size) {
            val curBucketValueCount = bucketsValueCount[bucketIndex]
            val curBucketLow = curBucketHig
            curBucketHig = bucketsHighBound[bucketIndex]

            if (curBucketValueCount == 0)
                continue

            countSeen += curBucketValueCount

            if (targetPos == countSeen)
                return calcPercentile(p, curBucketLow, curBucketHig)

            if (targetPos >= countSeen - curBucketValueCount + 1 && targetPos < countSeen)
                return calcPercentile(p, curBucketLow, curBucketHig)

            if (targetPos > countSeen - curBucketValueCount && targetPos < countSeen)
                return calcPercentile(p, prvBucketLow, curBucketHig)

            prvBucketLow = curBucketLow
        }

        return calcPercentile(p, prvBucketLow, curBucketHig)
    }


    private fun calcPercentile(p: Double, low: Double, high: Double): Double {
        val x = p / 100 * (valueCount - 1) + 1
        val part = x - floor(x)
        return low + (high - low) * part
    }

    /**
     * Add a new value to the population
     */
    fun addValue(v: Double) {
        require(v.isFinite())

        if (valueCount == 0) {
            initializeFirstValue(v)
            return
        }

        extendBounds(v)

        valueCount++

        // val range = v.floorResolution(precisionScale) - lowBoundInclusive
        // val curBucketIndex = (range / bucketSize).roundToInt()

        // avoid using slow floorResolution() as in the calculation above
        // instead estimate the bucket index and climb up until we reach the correct bucket
        val range = v - lowBoundInclusive
        var curBucketIndex = (range / bucketSize).toInt()
        while (true) {
            val curHighBoundExclusive = bucketsHighBound[curBucketIndex]
            val valueInCurrentBucket = v < curHighBoundExclusive
            if (valueInCurrentBucket)
                break
            curBucketIndex++
        }

        bucketsValueCount[curBucketIndex]++
    }

    /**
     * Calculate the Percentile Rank of the specified value, without adding it to the population.
     *
     * @return A percentile rank value in range 0 .. 100 or [Double.NaN] is population is empty.
     */
    fun getPercentileRank(v: Double): Double {
        require(v.isFinite())

        if (valueCount == 0)
            return Double.NaN

        if (v < lowBoundInclusive)
            return 0.0

        if (v >= higBoundExclusive)
            return 100.0

        var curBucketIndex = 0
        var countLess = 0
        while (true) {
            val curHighBoundExclusive = bucketsHighBound[curBucketIndex]
            val valueInCurrentBucket = v < curHighBoundExclusive
            if (valueInCurrentBucket)
                break

            countLess += bucketsValueCount[curBucketIndex]
            curBucketIndex++
        }


        val countEqual = bucketsValueCount[curBucketIndex]
        return (countLess + 0.5 * countEqual) / valueCount * 100
    }


    /**
     * Get the percentile rank of the specified value and after that, adds it to the population.
     */
    fun getRankThenAdd(v: Double): Double {
        val pof = getPercentileRank(v)
        addValue(v)
        return pof
    }


    private fun initializeFirstValue(v: Double) {
        lowBoundInclusive = v.floorResolution(precisionScale)
        higBoundExclusive = lowBoundInclusive + bucketSize
        higBoundExclusive = higBoundExclusive.roundResolution(precisionScale)
        bucketsValueCount.add(1)
        bucketsHighBound.add(higBoundExclusive)
        valueCount = 1
    }

    private fun extendBounds(v: Double) {
        if (v >= higBoundExclusive)
            extendHighBound(v)
        else if (v < lowBoundInclusive)
            extendLowBound(v)
    }

    private fun extendHighBound(v: Double) {
        higBoundExclusive = v.floorResolution(precisionScale) + bucketSize
        higBoundExclusive = higBoundExclusive.roundResolution(precisionScale)
        val range = higBoundExclusive - lowBoundInclusive
        val bucketCount = (range / bucketSize).roundToInt()
        val bucketsToAdd = bucketCount - bucketsValueCount.size
        bucketsValueCount.addAll(List(bucketsToAdd) { 0 })

        var curHighBound = bucketsHighBound.last()
        val highBounds = MutableList(bucketsToAdd) { 0.0 }
        for (i in 0 until highBounds.count()) {
            curHighBound += bucketSize
            curHighBound = curHighBound.roundResolution(precisionScale)
            highBounds[i] = curHighBound
        }
        bucketsHighBound.addAll(highBounds)
    }

    private fun extendLowBound(v: Double) {
        lowBoundInclusive = v.floorResolution(precisionScale)
        val range = higBoundExclusive - lowBoundInclusive
        val bucketCount = (range / bucketSize).roundToInt()
        val bucketsToAdd = bucketCount - bucketsValueCount.size
        bucketsValueCount.addAll(0, List(bucketsToAdd) { 0 })

        var curHighBound = lowBoundInclusive
        val highBounds = MutableList(bucketsToAdd) { 0.0 }
        for (i in 0 until highBounds.count()) {
            curHighBound += bucketSize
            curHighBound = curHighBound.roundResolution(precisionScale)
            highBounds[i] = curHighBound
        }
        bucketsHighBound.addAll(0, highBounds)
    }

    @Suppress("unused")
    fun getBucketsCopy(): List<Int> {
        return bucketsValueCount.toList()
    }
}

fun Double.floorResolution(scale: Int): Double {
    return Precision.round(this, scale, RoundingMode.FLOOR.ordinal)
}

fun Double.roundResolution(scale: Int): Double {
    return Precision.round(this, scale)
}
