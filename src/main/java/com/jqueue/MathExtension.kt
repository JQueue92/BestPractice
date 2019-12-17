package com.jqueue

import androidx.annotation.IntRange

fun formatDoubleXBit(value: Double, @IntRange(from = 0) bit: Int) = String.format("%.${bit}f", value)

fun Double.formatXBit(@IntRange(from = 0) bit: Int) = String.format("%.${bit}f", this)

fun formatFloatXBit(formatValue: Float, @IntRange(from = 0) bit: Int) = String.format("%.${bit}f", formatValue)

fun Float.formatXBit(@IntRange(from = 0) bit: Int) = String.format("%.${bit}f", this)
