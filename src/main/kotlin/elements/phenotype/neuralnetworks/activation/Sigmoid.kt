package elements.phenotype.neuralnetworks.activation

import org.apache.commons.math3.util.FastMath

/**
 * The Sigmoid function.
 *
 * Source: Page 146: Stanley, Kenneth Owen. Efficient evolution of neural networks through complexification. Diss. 2004.
 *
 * @property p Determines the slope of the curve. The slope increases proportionally to the value of p.
 * @constructor Creates a new instance of Sigmoid.
 */
open class Sigmoid(val p : Double = 4.9) : ActivationFunction() {

    override fun computeY(x: Double): Double {
        return 1.0 / (1.0 + FastMath.exp(-p * x))
    }
}