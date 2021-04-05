package sorting

import elements.FitnessElement

/**
 * Provides methods concerning the Pareto dominance.
 *
 */
class ParetoDominance {
    companion object {
        /**
         * Tests two objective vectors for Pareto dominance.
         *
         * @param a The first vector.
         * @param b The second vector.
         * @return Returns: -1 if a dominates b, 1 if b dominates a, and 0 otherwise.
         */
        fun dominanceTest(a: Array<Double>, b: Array<Double>): Int {
            return dominanceTest(a, b, 0)
        }

        /**
         * Tests two objective vectors for Pareto dominance.
         *
         * @param a The first vector.
         * @param b The second vector.
         * @param k First objective index to consider.
         * @return Returns: -1 if a dominates b, 1 if b dominates a, and 0 otherwise.
         */
        fun dominanceTest(a: Array<Double>, b: Array<Double>, k: Int): Int {
            var aDominates = false
            var bDominates = false

            var flag: Int

            for (i in k until a.size) {
                flag = a[i].compareTo(b[i])

                if (flag == -1) {
                    aDominates = true
                } else if (flag == 1) bDominates = true

                if (aDominates && bDominates) break
            }

            if (aDominates == bDominates) return 0

            return if (aDominates) -1 else 1
        }

        /**
         * Sorts the solutions of a set into non-dominated fronts. Based on the implementation of Deb et al.
         *
         * SOURCE: Deb, Kalyanmoy, et al. "A fast and elitist multiobjective genetic algorithm: NSGA-II." IEEE transactions on evolutionary computation 6.2 (2002): 182-197.
         *
         * @param input The index-fitness-vector pairs.
         * @return List of non-dominating fronts.
         */
        fun sortInNonDominatedFronts(input: Array<Pair<Int, Array<Double>>>): List<List<Pair<Int, Array<Double>>>> {
            data class FrontElement(val index: Int, val fitness: Array<Double>) {
                var dominatedBy = 0
                val dominates = mutableListOf<FrontElement>()

                override fun hashCode(): Int {
                    return index
                }

                override fun equals(other: Any?): Boolean {
                    return other is FrontElement && hashCode() == other.hashCode()
                }
            }

            val elements = mutableListOf<FrontElement>()

            for (pair in input) {
                elements.add(FrontElement(pair.first, pair.second))
            }

            for (i in 0 until elements.size - 1) {
                for (j in i + 1 until elements.size) {
                    val res = dominanceTest(
                        elements[i].fitness,
                        elements[j].fitness
                    )

                    if (res == 0) {
                        continue
                    }

                    val dominator: Int
                    val dominated: Int

                    if (res == -1) {
                        dominator = i
                        dominated = j
                    } else {
                        dominator = j
                        dominated = i
                    }

                    elements[dominator].dominates.add(elements[dominated])
                    elements[dominated].dominatedBy++
                }
            }

            val fronts = mutableListOf<List<Pair<Int, Array<Double>>>>()

            while (elements.isNotEmpty()) {
                val front = elements.filter { x -> x.dominatedBy == 0 }.toList()
                elements.removeAll(front)

                front.forEach { x -> x.dominates.forEach { y -> y.dominatedBy-- } }

                fronts.add(front.map { x -> Pair(x.index, x.fitness) }.toList())
            }

            return fronts
        }

        /**
         * Determines the non-dominated subset of a list of solutions.
         *
         * Similar to:
         * Mishra, K. K., and Sandeep Harit. "A fast algorithm for finding the non dominated set in multi objective optimization." International Journal of Computer Applications 1.25 (2010): 35-39.
         *
         * @param T The type of the fitness elements.
         * @param elements The list of solutions to filter.
         * @param k First objective index to consider.
         * @return
         */
        fun <T : FitnessElement> getNondominated(elements: List<T>, k: Int): List<T> {
            if (elements.isEmpty() || elements[0].fitness == null) {
                return mutableListOf()
            }

            /**
             * Stores which solutions are dominated.
             */
            val dominated = BooleanArray(elements.size)

            // Ignore solutions that have not got assigned a fitness value yet.
            for (i in elements.indices) {
                if (elements[i].fitness == null) {
                    dominated[i] = true
                }
            }

            for (i in elements.indices) {
                // If i is dominated, skip this iteration.
                if (dominated[i]) {
                    continue
                }
                for (j in i + 1 until elements.size) {
                    // If j is dominated, skip this iteration.
                    if (dominated[j]) {
                        continue
                    }

                    // Test for dominance.
                    val res = dominanceTest(
                        elements[i].fitness!!,
                        elements[j].fitness!!,
                        k
                    )

                    // Apply the necessary changes to _dominated_.
                    if (res == 1) {
                        dominated[i] = true

                        break
                    } else if (res == -1) {
                        dominated[j] = true
                    }
                }
            }

            // Filter for non-dominated solutions.
            val result = mutableListOf<T>()

            for (i in elements.indices) {
                if (!dominated[i]) {
                    result.add(elements[i])
                }
            }

            return result
        }
    }
}