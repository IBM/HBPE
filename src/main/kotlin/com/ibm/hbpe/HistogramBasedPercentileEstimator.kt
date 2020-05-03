/*
Copyright IBM Corporation 2020. All Rights Reserved
SPDX-License-Identifier: Apache-2.0

Description : Implementation of an Histogram-based percentile estimator
Author      : David Ohana (david.ohana@ibm.com)
*/

@file:Suppress("UnnecessaryVariable")

package com.ibm.hbpe

import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.roundToLong


class HistogramBasedPercentileEstimator(val bucketSize: Double = 0.1) {

    constructor(
        bucketSize: Double,
        sampleCount: Int,
        lowBoundInclusive: Double,
        higBoundExclusive: Double,
        buckets: List<Int>
    ) : this(bucketSize) {
        this.sampleCount = sampleCount
        this.lowBoundInclusive = lowBoundInclusive
        this.higBoundExclusive = higBoundExclusive
        this.buckets.addAll(buckets)
    }

    init {
        require(bucketSize.isFinite())
    }

    private val buckets = mutableListOf<Int>()

    var sampleCount = 0
        private set

    var lowBoundInclusive = Double.NaN
        private set

    var higBoundExclusive = Double.NaN
        private set

    fun clear() {
        buckets.clear()
        sampleCount = 0
        lowBoundInclusive = Double.NaN
        higBoundExclusive = Double.NaN
    }

    fun getPercentile(p: Double): Double {
        //todo: possible optimization: traverse in reverse if p > 50

        require(p.isFinite())
        require(p >= 0)
        require(p <= 100)

        if (sampleCount == 0)
            return Double.NaN

        if (p == 0.0)
            return lowBoundInclusive

        if (p == 100.0)
            return higBoundExclusive

        val targetPos = p / 100 * (sampleCount - 1) + 1
        var countSeen = 0.0
        var curBucketHig = lowBoundInclusive
        var prvBucketLow = lowBoundInclusive

        for (curBucketCount in buckets) {
            val curBucketLow = curBucketHig
            curBucketHig += bucketSize

            if (curBucketCount == 0)
                continue

            countSeen += curBucketCount

            if (targetPos == countSeen)
                return calcPercentile(p, curBucketLow, curBucketHig)

            if (targetPos >= countSeen - curBucketCount + 1 && targetPos < countSeen)
                return calcPercentile(p, curBucketLow, curBucketHig)

            if (targetPos > countSeen - curBucketCount && targetPos < countSeen)
                return calcPercentile(p, prvBucketLow, curBucketHig)

            prvBucketLow = curBucketLow
        }

        return calcPercentile(p, prvBucketLow, curBucketHig)
    }


    private fun calcPercentile(p: Double, low: Double, high: Double): Double {
        val x = p / 100 * (sampleCount - 1) + 1
        val part = x - floor(x)
        return low + (high - low) * part
    }

    fun addValue(v: Double) {
        require(v.isFinite())

        if (sampleCount == 0) {
            initializeFirstValue(v)
            return
        }

        extendBounds(v)

        sampleCount++
        val bucketIndex = ((v - lowBoundInclusive) / bucketSize).toInt()
        buckets[bucketIndex]++
    }

    fun getRank(v: Double): Double {
        require(v.isFinite())

        if (sampleCount == 0)
            return Double.NaN

        if (v < lowBoundInclusive)
            return 0.0

        if (v >= higBoundExclusive)
            return 100.0

        var curBucketIndex = 0
        var curHighBoundExclusive = lowBoundInclusive + bucketSize
        // workaround double precision issues
        curHighBoundExclusive = curHighBoundExclusive.floorTo(bucketSize)
        var countLess = 0
        while (v >= curHighBoundExclusive) {
            countLess += buckets[curBucketIndex]
            curBucketIndex++
            curHighBoundExclusive += bucketSize
            // workaround double precision issues
            curHighBoundExclusive = curHighBoundExclusive.floorTo(bucketSize)
        }

        if (curBucketIndex >= buckets.size) {
            println(bucketSize)
            println(lowBoundInclusive)
            println(curHighBoundExclusive)
            println(lowBoundInclusive + bucketSize)
            println(v)
            println(curBucketIndex)
            println(buckets.size)
            println(countLess)
        }

        val countEqual = buckets[curBucketIndex]
        return (countLess + 0.5 * countEqual) / sampleCount * 100
    }

    fun getRankThenAdd(v: Double): Double {
        val pof = getRank(v)
        addValue(v)
        return pof
    }


    private fun initializeFirstValue(v: Double) {
        lowBoundInclusive = v.floorTo(bucketSize)
        higBoundExclusive = lowBoundInclusive + bucketSize
        buckets.add(1)
        sampleCount = 1
    }

    private fun extendBounds(v: Double) {
        if (v >= higBoundExclusive)
            extendHighBound(v)
        else if (v < lowBoundInclusive)
            extendLowBound(v)
    }

    private fun extendHighBound(v: Double) {
        higBoundExclusive = v.floorTo(bucketSize) + bucketSize
        val bucketCount = ((higBoundExclusive - lowBoundInclusive) / bucketSize).roundToInt()
        val bucketsToAdd = bucketCount - buckets.size
        buckets.addAll(List(bucketsToAdd) { 0 })
    }

    private fun extendLowBound(v: Double) {
        lowBoundInclusive = v.floorTo(bucketSize)
        val bucketCount = ((higBoundExclusive - lowBoundInclusive) / bucketSize).roundToInt()
        val bucketsToAdd = bucketCount - buckets.size
        buckets.addAll(0, List(bucketsToAdd) { 0 })
    }

    fun getBucketsCopy(): List<Int> {
        return buckets.toList()
    }
}

fun Double.fixPrecision(): Double = (this * 1000000000).roundToLong() / 1000000000.0


fun Double.floorTo(precision: Double): Double {
    // round after the 9th digit to avoid effects of double precision loss

    val a = this / precision
    val b = a.fixPrecision()
    val c = floor(b)
    val d = c * precision
    val e = d.fixPrecision()
    return e
//    val fl = floor(this / precision) * precision
//    return fl.fixPrecision()
}
