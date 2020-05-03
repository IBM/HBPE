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


class HistogramBasedPercentileEstimator(val bucketScale: Int = 1) {
    val bucketSize = 10.0.pow(-bucketScale)

    @Suppress("unused")
    constructor(
        bucketScale: Int,
        valueCount: Int,
        lowBoundInclusive: Double,
        higBoundExclusive: Double,
        buckets: List<Int>
    ) : this(bucketScale) {
        this.valueCount = valueCount
        this.lowBoundInclusive = lowBoundInclusive
        this.higBoundExclusive = higBoundExclusive
        this.bucketsValueCount.addAll(buckets)
    }

    private val bucketsValueCount = mutableListOf<Int>()
    private val bucketsHighBound = mutableListOf<Double>()

    var valueCount = 0
        private set

    var lowBoundInclusive = Double.NaN
        private set

    var higBoundExclusive = Double.NaN
        private set

    fun clear() {
        valueCount = 0
        lowBoundInclusive = Double.NaN
        higBoundExclusive = Double.NaN
        bucketsValueCount.clear()
        bucketsHighBound.clear()
    }

    fun getPercentile(p: Double): Double {
        //todo: possible optimization: traverse in reverse if p > 50

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

    fun addValue(v: Double) {
        require(v.isFinite())

        if (valueCount == 0) {
            initializeFirstValue(v)
            return
        }

        extendBounds(v)

        valueCount++
        val range = v.floorResolution(bucketScale) - lowBoundInclusive
        val bucketIndex = (range / bucketSize).roundToInt()

        bucketsValueCount[bucketIndex]++
    }

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

    fun getRankThenAdd(v: Double): Double {
        val pof = getPercentileRank(v)
        addValue(v)
        return pof
    }


    private fun initializeFirstValue(v: Double) {
        lowBoundInclusive = v.floorResolution(bucketScale)
        higBoundExclusive = lowBoundInclusive + bucketSize
        higBoundExclusive = higBoundExclusive.roundResolution(bucketScale)
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
        higBoundExclusive = v.floorResolution(bucketScale) + bucketSize
        higBoundExclusive = higBoundExclusive.roundResolution(bucketScale)
        val range = higBoundExclusive - lowBoundInclusive
        val bucketCount = (range / bucketSize).roundToInt()
        val bucketsToAdd = bucketCount - bucketsValueCount.size
        bucketsValueCount.addAll(List(bucketsToAdd) { 0 })

        var curHighBound = bucketsHighBound.last()
        val highBounds = MutableList(bucketsToAdd) { 0.0 }
        for (i in 0 until highBounds.count()) {
            curHighBound += bucketSize
            curHighBound = curHighBound.roundResolution(bucketScale)
            highBounds[i] = curHighBound
        }
        bucketsHighBound.addAll(highBounds)
    }

    private fun extendLowBound(v: Double) {
        lowBoundInclusive = v.floorResolution(bucketScale)
        val range = higBoundExclusive - lowBoundInclusive
        val bucketCount = (range / bucketSize).roundToInt()
        val bucketsToAdd = bucketCount - bucketsValueCount.size
        bucketsValueCount.addAll(0, List(bucketsToAdd) { 0 })

        var curHighBound = lowBoundInclusive
        val highBounds = MutableList(bucketsToAdd) { 0.0 }
        for (i in 0 until highBounds.count()) {
            curHighBound += bucketSize
            curHighBound = curHighBound.roundResolution(bucketScale)
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
