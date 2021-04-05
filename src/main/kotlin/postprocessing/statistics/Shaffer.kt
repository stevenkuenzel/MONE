package postprocessing.statistics

import java.util.ArrayList
import org.apache.commons.math3.stat.ranking.TiesStrategy
import org.apache.commons.math3.stat.ranking.NaNStrategy
import org.apache.commons.math3.stat.ranking.NaturalRanking
import org.apache.commons.math3.util.FastMath

/**
 * Shaffer's algorithm for p-value-correction.
 */
class Shaffer {
    companion object {
        fun correct(input: Array<Array<Double>>): Array<Array<Double>> {
            val k = input.size
            val pairs = TwoStageTest.generatePairs(k)
            val rawPValues = DoubleArray(pairs.size) { i -> input[pairs[i].first][pairs[i].second] }

            val sk = countRecursively(k)
            sk.removeAt(0)

            val iDiff = diff(sk)

            // Get the number of hypothesis that can be simultaneously true for each number of rejected hypothesis
            val last = sk.removeAt(sk.size - 1)

            val ti = rep(sk, iDiff)
            ti.add(last)
            ti.reverse()

            // Order the p-values to apply the correction and correct them with the number of hypothesis that can be simultaneously true
            val ranking = NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.AVERAGE)

            val o = ranking.rank(rawPValues)
            val o1 = Array(o.size) { i -> o[i].toInt() - 1 }

            var adjPValues = applyOrder(rawPValues, o1, false)
            for (i in ti.indices) {
                adjPValues[i] = adjPValues[i] * ti[i].toDouble()
            }
            for (i in adjPValues.indices) {
                adjPValues[i] = FastMath.min(adjPValues[i], 1.0)
            }
            for (i in adjPValues.indices) {
                adjPValues[i] = max(adjPValues, i)
            }

            adjPValues = applyOrder(adjPValues, o1, true)

            val output = Array(input.size) { Array(input[0].size) { 0.0 } }

            for (i in pairs.indices) {
                val pair = pairs[i]
                output[pair.first][pair.second] = adjPValues[i]
                output[pair.second][pair.first] = adjPValues[i]
            }

            return output
        }

        /*
        AUXILIARY FUNCTIONS.
         */

        private fun countRecursively(k: Int): MutableList<Int> {
            val res = ArrayList<Int>()

            res.add(0)

            if (k > 1) {

                val tmp0 = countRecursively(k - 1)

                for (v in tmp0) {
                    if (!res.contains(v)) {
                        res.add(v)
                    }
                }

                for (i in 2..k) {
                    val tmp = countRecursively(k - i)

                    val fact = factorial(i) / (2 * factorial(i - 2))

                    for (j in tmp.indices) {
                        tmp[j] = tmp[j] + fact
                    }

                    for (v in tmp) {
                        if (!res.contains(v)) {
                            res.add(v)
                        }
                    }
                }
            }

            res.sort()

            return res
        }

        private fun factorial(num_: Int): Int {
            var num = num_
            var result = 1

            while (num > 1) {
                result *= num--
            }

            return result
        }

        private fun diff(input: List<Int>): Array<Int> {
            val output = Array(input.size - 1) { 0 }

            for (i in 1 until input.size) {
                output[i - 1] = input[i] - input[i - 1]
            }

            return output
        }

        private fun rep(input: List<Int>, count: Array<Int>): MutableList<Int> {
            assert(input.size == count.size)

            val output = ArrayList<Int>()

            for (i in input.indices) {
                for (j in 0 until count[i]) {
                    output.add(input[i])
                }
            }

            return output
        }


        private fun applyOrder(input: DoubleArray, order: Array<Int>, reverse: Boolean): DoubleArray {
            assert(input.size == order.size)

            val result = DoubleArray(input.size) { 0.0 }

            for (i in input.indices) {
                if (reverse) {
                    result[i] = input[order[i]]
                } else {
                    result[order[i]] = input[i]
                }
            }

            return result
        }

        private fun max(input: DoubleArray, maxIndex: Int): Double {
            var max = 0.0

            for (i in 0..FastMath.min(input.size - 1, maxIndex)) {
                val v = input[i]

                if (v > max) {
                    max = v
                }
            }

            return max
        }
    }
}