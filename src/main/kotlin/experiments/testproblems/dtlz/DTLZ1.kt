package experiments.testproblems.dtlz

import experiments.testproblems.TestProblem
import kotlin.math.cos

class DTLZ1(id : Int, numOfVariables: Int = 7, numOfObjectives: Int = 3) : TestProblem(id, numOfVariables, numOfObjectives) {
    override val name = "DTLZ1"

    override fun evaluate(x: Array<Double>): Array<Double> {
        val k = numberOfVariables - numberOfObjectives + 1
        val f = Array(numberOfObjectives) { 1.0 }

        var g = 0.0

        for (i in numberOfVariables - k until numberOfVariables) {
            g += (x[i] - 0.5) * (x[i] - 0.5) - cos(20.0 * Math.PI * (x[i] - 0.5))
        }

        g = 100.0 * (k + g)
        for (i in 0 until numberOfObjectives) {
            f[i] = (1.0 + g) * 0.5
        }

        for (i in 0 until numberOfObjectives) {
            for (j in 0 until numberOfObjectives - (i + 1)) {
                f[i] *= x[j]
            }
            if (i != 0) {
                val aux = numberOfObjectives - (i + 1)
                f[i] *= 1.0 - x[aux]
            }
        }

        return f
    }
}