package elements.phenotype.neuralnetworks.activation

import org.apache.commons.math3.util.FastMath

/**
 * The Elliot Sigmoid function.
 *
 * Source: Elliott, David L. A better activation function for artificial neural networks. 1993. Technical Report, URL: https://drum.lib.umd.edu/handle/1903/5355, 2021-02-26.
 *
 * @property p Determines the slope of the curve. The slope increases proportionally to the value of p.
 * @constructor Creates a new instance of ElliotSigmoid.
 */
class ElliotSigmoid(p : Double) : Sigmoid(p) {
    constructor() : this(4.9)

    override fun computeY(x: Double): Double {
        val px = p * x

        return 0.5 * (px / (1.0 + FastMath.abs(px)) + 1.0)
    }
}