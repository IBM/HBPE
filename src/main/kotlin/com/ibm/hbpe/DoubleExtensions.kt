package com.ibm.hbpe

import org.apache.commons.math3.util.Precision
import java.math.RoundingMode

fun Double.floorResolution(scale: Int): Double {
    return Precision.round(this, scale, RoundingMode.FLOOR.ordinal)
}

fun Double.roundResolution(scale: Int): Double {
    return Precision.round(this, scale)
}
