package experiments.testproblems

import elements.phenotype.neuralnetworks.NetworkPhenotype
import experiments.Experiment
import experiments.Reference
import experiments.SampleVector

abstract class TestProblem(id: Int, numOfVariables: Int, numOfObjectives: Int) : Experiment(
    id, numOfVariables, numOfObjectives,  false,
    false, true
) {
    val numberOfVariables = numOfInputs
    val numberOfObjectives = numOfOutputs

    override val evaluateEachSolutionOnce = true

    abstract fun evaluate(x: Array<Double>): Array<Double>

//    fun getMax(iterations: Int = 1000): Array<Double> {
//        val max = Array(numberOfObjectives)
//        {
//            Double.NEGATIVE_INFINITY
//        }
//
//
//        for (i in 0 until iterations) {
//            val sample = Array(numberOfVariables) { random.nextDouble() }
//
//            val fitness = evaluate(sample)
//
//            for (index in fitness.indices) {
//                if (fitness[index] > max[index]) max[index] = fitness[index]
//            }
//        }
//
//        return max
//    }

    override fun sampleAgainst(phenotype: NetworkPhenotype, reference: Reference): SampleVector {
        TODO("Not yet implemented")
    }

    override fun evaluate(networkPhenotype: NetworkPhenotype, referenceID: Int) {
        TODO("Not yet implemented")
    }
}