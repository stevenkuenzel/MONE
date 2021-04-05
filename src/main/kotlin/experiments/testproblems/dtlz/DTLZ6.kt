package experiments.testproblems.dtlz

import experiments.testproblems.TestProblem

class DTLZ6(id : Int, numOfVariables: Int = 12, numOfObjectives: Int = 3) : TestProblem(id, numOfVariables, numOfObjectives) {
    override val name = "DTLZ6"

    override fun evaluate(x: Array<Double>): Array<Double> {
        val theta = DoubleArray(numberOfObjectives - 1)

        val f = Array(numberOfObjectives) { 1.0 }

        val k = numberOfVariables - numberOfObjectives + 1

        var g = 0.0
        for (i in numberOfVariables - k until numberOfVariables) {
            g += Math.pow(x[i], 0.1)
        }

        val t = Math.PI / (4.0 * (1.0 + g))
        theta[0] = x[0] * Math.PI / 2
        for (i in 1 until numberOfObjectives - 1) {
            theta[i] = t * (1.0 + 2.0 * g * x[i])
        }

        for (i in 0 until numberOfObjectives) {
            f[i] = 1.0 + g
        }

        for (i in 0 until numberOfObjectives) {
            for (j in 0 until numberOfObjectives - (i + 1)) {
                f[i] *= Math.cos(theta[j])
            }
            if (i != 0) {
                val aux = numberOfObjectives - (i + 1)
                f[i] *= Math.sin(theta[aux])
            }
        }

        return f
    }
}