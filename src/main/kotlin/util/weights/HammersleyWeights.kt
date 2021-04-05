package util.weights

import org.apache.commons.math3.primes.Primes
import org.apache.commons.math3.util.FastMath
import java.util.ArrayList

/**
 * Generate uniform design using Hammersley method:
 *
 * SOURCE: Berenguer, José A. Molinet, and Carlos A. Coello Coello. "Evolutionary many-objective optimization based on kuhn-munkres’ algorithm." International Conference on Evolutionary Multi-Criterion Optimization. Springer, Cham, 2015.
 */
class HammersleyWeights : WeightGenerator() {
    override fun generateWeights(numOfVectors: Int, numOfObjectives: Int): Array<Array<Double>> {
        val designs = ArrayList<Array<Double>>()
        val primes = generateFirstKPrimes(numOfObjectives - 2)

        // Create K-1 dimensional designs.
        for (i in 0 until numOfVectors) {
            val design = Array(numOfObjectives - 1) { 0.0 }
            design[0] = (2.0 * (i + 1) - 1.0) / (2.0 * numOfVectors)

            for (j in 1 until numOfObjectives - 1) {
                var f = 1.0 / primes[j - 1]
                var d = i + 1
                design[j] = 0.0

                while (d > 0) {
                    design[j] += f * (d % primes[j - 1])
                    d /= primes[j - 1]
                    f /= primes[j - 1]
                }
            }

            designs.add(design)
        }

        // Create the weights.
        val weights = ArrayList<Array<Double>>()

        for (design in designs) {
            val weight = Array(numOfObjectives) { 0.0 }

            for (i in 1..numOfObjectives) {
                if (i == numOfObjectives) {
                    weight[i - 1] = 1.0
                } else {
                    weight[i - 1] = 1.0 - FastMath.pow(design[i - 1], 1.0 / (numOfObjectives - i))
                }

                for (j in 1 until i) {
                    weight[i - 1] *= FastMath.pow(design[j - 1], 1.0 / (numOfObjectives - j))
                }
            }

            weights.add(weight)
        }

        // Convert into an array.
        val weightVectors = Array(numOfVectors) { Array(numOfObjectives) { 0.0 } }

        for (n in weightVectors.indices) {
            weightVectors[n] = weights[n]
        }

        return weightVectors
    }

    /**
     * Generates the first _k_ prime numbers.
     */
    private fun generateFirstKPrimes(k: Int): Array<Int> {
        if (k < 1) {
            return Array(0) { 0 }
        }

        val primes = Array(k) { 0 }
        primes[0] = 2

        for (i in 1 until k) {
            primes[i] = Primes.nextPrime(primes[i - 1])
        }

        return primes
    }
}