package experiments.testproblems.dtlz

import experiments.testproblems.TestProblem
import kotlin.math.cos
import kotlin.math.sin

class DTLZ3(id : Int, numOfVariables: Int = 12, numOfObjectives: Int = 3) : TestProblem(id, numOfVariables, numOfObjectives) {
    override val name = "DTLZ3"

    override fun evaluate(x: Array<Double>): Array<Double> {
        val f = Array(numberOfObjectives) { 1.0 }
        val k = numberOfVariables - numberOfObjectives + 1

        var g = 0.0
        for (i in numberOfVariables - k until numberOfVariables) {
            g += (x[i] - 0.5) * (x[i] - 0.5) - cos(20.0 * Math.PI * (x[i] - 0.5))
        }

        g = 100.0 * (k + g)
        for (i in 0 until numberOfObjectives) {
            f[i] = 1.0 + g
        }

        for (i in 0 until numberOfObjectives) {
            for (j in 0 until numberOfObjectives - (i + 1)) {
                f[i] *= cos(x[j] * 0.5 * Math.PI)
            }
            if (i != 0) {
                val aux = numberOfObjectives - (i + 1)
                f[i] *= sin(x[aux] * 0.5 * Math.PI)
            }
        }

        return f
    }
}