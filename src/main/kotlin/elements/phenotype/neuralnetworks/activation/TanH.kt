package elements.phenotype.neuralnetworks.activation

import kotlin.math.tanh

/**
 * The Hyperbolic Tangent function.
 *
 * Source: https://mathworld.wolfram.com/HyperbolicTangent.html, 2021-02-26.
 *
 * @param p Determines the slope of the curve. The slope increases proportionally to the value of p.
 * @constructor Creates a new instance of TanH.
 */
class TanH(p : Double) : ActivationFunction() {
    private val pHalf = p * 0.5

    override fun computeY(x: Double): Double {

        return 0.5 * (tanh(pHalf * x) + 1.0)
    }
}