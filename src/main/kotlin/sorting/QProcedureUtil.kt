package sorting

import kotlin.math.abs
import kotlin.math.max

/**
 * Counter for the unique q-Procedure ID.
 */
var INSTANCE_COUNTER = 0

/**
 * Returns true, if two double values are equal w.r.t. a certain epsilon.
 *
 * @param other The other double value.
 */
fun Double.equalsDelta(other: Double) = abs(this - other) < max(Math.ulp(this), Math.ulp(other)) * 2

/**
 * Returns true, if two double values are inequal w.r.t. a certain epsilon.
 *
 * @param other The other double value.
 */
fun Double.notEqualsDelta(other: Double) = !this.equalsDelta(other)