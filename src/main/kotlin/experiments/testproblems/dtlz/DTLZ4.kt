package experiments.testproblems.dtlz

import experiments.testproblems.TestProblem
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

class DTLZ4(id : Int, numOfVariables: Int = 12, numOfObjectives: Int = 3) : TestProblem(id, numOfVariables, numOfObjectives) {
    override val name = "DTLZ4"

    override fun evaluate(x: Array<Double>): Array<Double> {
        val alpha = 100.0

        val f = Array(numberOfObjectives) { 1.0 }
        val k = numberOfVariables - numberOfObjectives + 1

        var g = 0.0
        for (i in numberOfVariables - k until numberOfVariables) {
            g += (x[i] - 0.5) * (x[i] - 0.5)
        }

        for (i in 0 until numberOfObjectives) {
            f[i] = 1.0 + g
        }

        for (i in 0 until numberOfObjectives) {
            for (j in 0 until numberOfObjectives - (i + 1)) {
                f[i] *= cos(x[j].pow(alpha) * (Math.PI / 2.0))
            }
            if (i != 0) {
                val aux = numberOfObjectives - (i + 1)
                f[i] *= sin(x[aux].pow(alpha) * (Math.PI / 2.0))
            }
        }

        return f
    }
}