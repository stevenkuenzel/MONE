package experiments.testproblems.dtlz

import experiments.testproblems.TestProblem


class DTLZ7(id : Int, numOfVariables: Int = 22, numOfObjectives: Int = 3) : TestProblem(id, numOfVariables, numOfObjectives) {
    override val name = "DTLZ7"

    override fun evaluate(x: Array<Double>): Array<Double> {

        val f = Array(numberOfObjectives) { 1.0 }
        val k = numberOfVariables - numberOfObjectives + 1

        var g = 0.0
        for (i in numberOfVariables - k until numberOfVariables) {
            g += x[i]
        }

        g = 1 + 9.0 * g / k

        System.arraycopy(x, 0, f, 0, numberOfObjectives - 1)

        var h = 0.0
        for (i in 0 until numberOfObjectives - 1) {
            h += f[i] / (1.0 + g) * (1 + Math.sin(3.0 * Math.PI * f[i]))
        }

        h = numberOfObjectives - h

        f[numberOfObjectives - 1] = (1 + g) * h

        return f
    }
}