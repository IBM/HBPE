package com.ibm.hbpe

import org.apache.commons.math3.stat.descriptive.rank.Percentile
import org.apache.commons.math3.stat.ranking.NaNStrategy
import org.apache.commons.math3.util.KthSelector
import org.apache.commons.math3.util.MedianOf3PivotingStrategy

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