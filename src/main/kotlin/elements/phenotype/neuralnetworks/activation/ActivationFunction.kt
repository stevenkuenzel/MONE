package elements.phenotype.neuralnetworks.activation

/**
 * An abstract activation function.
 *
 * @constructor Create empty Activation function
 */
abstract class ActivationFunction {
    /**
     * Determines the value f(x) for a given input x.
     *
     * @param x The input to the activation function.
     * @return The value f(x).
     */
    abstract fun computeY(x : Double) : Double
}