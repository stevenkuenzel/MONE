package experiments.testproblems.dtlz

import experiments.testproblems.TestProblem
import kotlin.math.cos
import kotlin.math.sin

class DTLZ5(id : Int, numOfVariables: Int = 12, numOfObjectives: Int = 3) : TestProblem(id, numOfVariables, numOfObjectives) {
    override val name = "DTLZ5"

    override fun evaluate(x: Array<Double>): Array<Double> {
        val theta = DoubleArray(numberOfObjectives - 1)
        var g = 0.0

        val f = Array(numberOfObjectives) { 1.0 }
        val k = numberOfVariables - numberOfObjectives + 1


        for (i in numberOfVariables - k until numberOfVariables) {
            g += (x[i] - 0.5) * (x[i] - 0.5)
        }

        val t = Math.PI / (4.0 * (1.0 + g))

        theta[0] = x[0] * Math.PI / 2.0
        for (i in 1 until numberOfObjectives - 1) {
            theta[i] = t * (1.0 + 2.0 * g * x[i])
        }

        for (i in 0 until numberOfObjectives) {
            f[i] = 1.0 + g
        }

        for (i in 0 until numberOfObjectives) {
            for (j in 0 until numberOfObjectives - (i + 1)) {
                f[i] *= cos(theta[j])
            }
            if (i != 0) {
                val aux = numberOfObjectives - (i + 1)
                f[i] *= sin(theta[aux])
            }
        }

        return f
    }
}