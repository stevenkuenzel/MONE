package util

import util.random.RandomProvider

/**
 * Provides different approaches for selecting solutions.
 */
class Selection {
    companion object {
        /**
         * Selects one index according to the provided selection probabilities. Roulette Wheel Selection.
         *
         * @param distr Selection probabilities.
         * @param random Random number generator.
         * @return Selected index.
         */
        fun rouletteWheel(distr: Array<Double>, random: RandomProvider): Int {
            var value = random.nextDouble()

            for (index in distr.size - 1 downTo 0) {
                value -= distr[index]

                if (value <= 0.0) {
                    return index
                }
            }

            return -1
        }

        /**
         * Creates a rank based (linear) selection probability distribution and returns one or more selected indices. Stochastic Universal Sampling.
         *
         * @param min Minimum index.
         * @param max Maximum index.
         * @param amount Number of indices to select.
         * @param selectionPressure Selection pressure.
         * @param random Random number generator.
         * @return The selected indices.
         */
        fun selectIndices(
            min: Int,
            max: Int,
            amount: Int,
            selectionPressure: Double,
            random: RandomProvider
        ): Array<Int> {
            val steps = max - min + 1

            val tmpResult = selectIndices(
                linearDistribution(
                    steps,
                    selectionPressure
                ), amount, random
            )

            return Array(tmpResult.size) { i -> tmpResult[i] + min }
        }

        /**
         * Selects one or more indices according to the provided selection probabilities. Stochastic Universal Sampling.
         *
         * @param distribution Selection probabilities.
         * @param amount Number of indices to select.
         * @param random Random number generator.
         * @return The selected indices.
         */
        fun selectIndices(distribution: Array<Double>, amount: Int, random: RandomProvider): Array<Int> {
            val result = Array(amount) { -1 }

            var current = 0
            var i = 0

            val stepSize = 1.0 / amount.toDouble()
            var r = random.nextDouble() * stepSize

            while (current < amount) {
                val index = distribution.size - (i + 1)

                while (r <= distribution[index]) {
                    result[current++] = index

                    if (current == amount) {
                        return result
                    }

                    r += stepSize
                }
                i++
            }

            return result
        }

        /**
        * Creates an array linearly decreasing selection probabilities.
         *
         * @param steps Number of entries / steps.
         * @param selectionPressure Selection pressure.
         * @param additive If true, every value is added with the next lower one.
         * @return Selection probabilities.
         */
        fun linearDistribution(steps: Int, selectionPressure: Double, additive: Boolean = true): Array<Double> {
            val s = 1.0 + selectionPressure

            val f1 = (2.0 - s) / steps.toDouble()
            val f2 = (2.0 * (s - 1.0)) / (steps.toDouble() * (steps - 1).toDouble())

            val distribution = Array(steps) { i -> f1 + f2 * (steps - (i + 1)).toDouble() }

            if (additive) {
                var prev = 0.0

                for (i in steps - 1 downTo 0) {
                    val tmp = distribution[i]
                    distribution[i] += prev
                    prev += tmp
                }
            }

            return distribution
        }

        /**
         * Creates an array linearly decreasing selection probabilities.
         *
         * @param steps Number of entries / steps.
         * @return Selection probabilities.
         */
        fun equalDistribution(steps: Int): Array<Double> {
            val f1 = 1.0 / steps.toDouble()

            val distribution = Array(steps) { f1 }

            var prev = 0.0

            for (i in steps - 1 downTo 0) {
                val tmp = distribution[i]
                distribution[i] += prev
                prev += tmp
            }

            return distribution
        }
    }
}