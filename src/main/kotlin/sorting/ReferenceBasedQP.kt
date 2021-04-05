package sorting


/**
 * q-Procedure that requires a reference point for computing q-Values.
 *
 * @constructor Creates a new instance.
 *
 * @param iterative If true, it runs in iterative mode.
 * @param next The subordinate q-Procedure. Not mandatory.
 */
abstract class ReferenceBasedQP(iterative: Boolean, next: QProcedure?) : QProcedure(iterative, next) {
    /**
     * The reference point.
     */
    var referencePoint = Array(0) { 0.0 }

    /**
     * Factor for correcting the resulting q-Values according to the reference point.
     */
    var referencePointCorrectionFactor = 1.0

    /**
     * Does the reference point have to be updated before q-Values are computed?
     */
    var updateEveryCall = true

    /**
     * Update reference point upon a specific procedure.
     */
    abstract fun updateReferencePoint(point: Array<Double>, setSize: Int)

    /**
     * Updates the reference point with equal values for each dimension.
     *
     * @param dimensions Number of considered dimensions.
     * @param value Value for each dimension.
     */
    fun updateReferencePoint(dimensions: Int, value: Double) {
        referencePoint = Array(dimensions) { value }

        referencePointCorrectionFactor = 1.0

        for (i in 0 until dimensions) {
            referencePointCorrectionFactor *= referencePoint[i]
        }
    }
}