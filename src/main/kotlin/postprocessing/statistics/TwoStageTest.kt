package postprocessing.statistics

import org.apache.commons.math3.distribution.ChiSquaredDistribution
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.LUDecomposition
import org.apache.commons.math3.util.FastMath
import java.util.ArrayList
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.linear.SingularMatrixException

/**
 * Performs a two-stage-test for statistical significance.
 *
 * 1. Stage: Is there any difference among > 2 different treatments?
 *
 * 2. Stage: Compare the treatments pairwise for statistical significance.
 *
 * @property data The test data.
 * @property alpha The significance level.
 * @constructor Creates a new instance.
 */
class TwoStageTest(val data: StatData, private val alpha: Double = 0.05) {
    companion object {
        fun generatePairs(k: Int): List<Pair<Int, Int>> {
            val result = ArrayList<Pair<Int, Int>>()

            for (i in 0 until k) {
                for (j in i + 1 until k) {
                    result.add(Pair(i, j))
                }
            }

            return result
        }
    }

    /**
     * The test data transformed into ranked data.
     */
    private val dataRanked = data.getRanked()

    /**
     * Performs a two-stage-test on _data_ and returns a matrix of strings denoting significant differences.
     *
     *
     * @param invert If true, the resulting matrix describes pairs where no significant difference exists.
     * @return List of pairwise difference strings.
     */
    fun apply(invert: Boolean = false): Array<String> {

        if (data.isEmpty() || dataRanked.isEmpty()) {
            println("Data is empty.")
            return Array(0) { "" }
        }

        // 1. Stage: Friedman or Skillings-Mack.
        val comparePairwise = when {
            data.numOfTreatments == 2 -> true
            data.isComplete() -> friedman(dataRanked) < alpha
            else -> skillingsMack(dataRanked) < alpha
        }

        if (!comparePairwise) {
            println("No pairwise comparison necessary.")
            return Array(data.numOfTreatments) { "" }
        }

        println("Doing pairwise comparison.")


        // 2. Stage: Friedman Post-Hoc or Least Squared Distances. Incl. p value correction.
        val diff = if (data.isComplete()) {
            friedmanPostHoc(dataRanked)
        } else {
            skillingsMackPostHoc(dataRanked)
        }

        // Create the pairwise difference strings.
        val result = Array(diff.size) { "" }

        val strSmaller = "\\textsuperscript{$<$}"
        val strGreater = "\\textsuperscript{$>$}"

        for (i in diff.indices) {
            for (j in i + 1 until diff.size) {
                if ((diff[i][j] != TestResult.Indifferent && !invert) || (diff[i][j] == TestResult.Indifferent && invert)) {

                    val prefixI = if (diff[i][j] == TestResult.Smaller) strSmaller else strGreater
                    val prefixJ = if (diff[j][i] == TestResult.Smaller) strSmaller else strGreater

                    result[i] += "${j + 1}$prefixI "
                    result[j] += "${i + 1}$prefixJ "
                }
            }
        }

        for (i in result.indices) {
            result[i] = result[i].trim().replace(" ", ", ")
        }

        return result
    }

    /**
     * Friedman test.
     *
     * H0 = No significant differenceMeasure. p >= alpha
     * H1 = Significant differenceMeasure. p < alpha
     *
     */
    private fun friedman(dataRanked: StatData): Double {
        val k = dataRanked.getK()
        val n = dataRanked.getN()

        val chiSquaredDistribution = ChiSquaredDistribution((k - 1).toDouble())

        val meanRank = Array(k) { i -> dataRanked.getMeanOfTreatment(i) }

        var squaredMeanRankSum = 0.0

        for (v in meanRank) {
            squaredMeanRankSum += v * v
        }

        val friedmanStat = 12.0 * n / (k * (k + 1.0)) * (squaredMeanRankSum - k * ((k + 1.0) * (k + 1.0)) / 4.0)

        val p = 1.0 - chiSquaredDistribution.cumulativeProbability(friedmanStat)

        return p
    }

    /**
     * Skillings–Mack test (Friedman test when there are missing data).
     *
     * SOURCE: https://www.ncbi.nlm.nih.gov/pmc/articles/PMC2761045
     */
    private fun skillingsMack(dataRanked: StatData): Double {
        val ranked = skillingsMackPrepareData(dataRanked)

        val A = Array(ranked.numOfTreatments - 1) { t ->
            var sum = 0.0

            for (b in 0 until ranked.numOfBlocks) {
                sum += FastMath.sqrt(12.0 / (ranked.getNumOfTreatmentsInBlock(b) + 1).toDouble()) *
                        (ranked.getValue(t, b) - (ranked.getNumOfTreatmentsInBlock(b) + 1).toDouble() / 2.0)
            }

            sum
        }

        val sig = Array(ranked.getK()) { DoubleArray(ranked.getK()) }

        for (i in sig.indices) {

            var sum = 0.0

            for (t in 0 until ranked.getK()) {
                if (t == i) continue

                sum += ranked.getNumBothTreatmentsInBlock(i, t)
            }

            sig[i][i] = sum

            for (j in i + 1 until sig.size) {
                sig[i][j] = (-ranked.getNumBothTreatmentsInBlock(i, j)).toDouble()
                sig[j][i] = sig[i][j]
            }
        }

        val sigMatrix = Array2DRowRealMatrix(sig)

        try {
            val sigMatrixInv = LUDecomposition(sigMatrix).solver.inverse
            val pVector = sigMatrixInv.preMultiply(ArrayRealVector(A))

            val pVectorArray = pVector.toArray()
            var sm = 0.0

            for (i in pVectorArray.indices) {
                sm += pVectorArray[i] * A[i]
            }

            val chiSquaredDistribution = ChiSquaredDistribution((ranked.getK() - 1).toDouble())
            return 1.0 - chiSquaredDistribution.cumulativeProbability(sm)
        } catch (ex: SingularMatrixException) {

        }

        return 1.0
    }

    /**
     * Fills empty blocks of _dataRanked_ according to the definition of the Skillings-Mack test.
     */
    private fun skillingsMackPrepareData(dataRanked: StatData): StatData {
        val ranked = StatData(dataRanked)

        for (b in 0 until ranked.numOfBlocks) {
            val numOfTreatmentsInBlock = ranked.getNumOfTreatmentsInBlock(b)

            if (numOfTreatmentsInBlock < ranked.numOfTreatments) {
                for (t in 0 until ranked.numOfTreatments) {
                    if (!ranked.isPresent(t, b)) {
                        ranked.setValue(t, b, (numOfTreatmentsInBlock + 1).toDouble() / 2.0, true)
                    }
                }
            }
        }

        return ranked
    }

    /**
     * Friedman (pairwise) post-hoc test.
     */
    private fun friedmanPostHoc(dataRanked: StatData): Array<Array<TestResult>> {
        val normalDistribution = NormalDistribution()

        val k = dataRanked.getK()
        val n = dataRanked.getN()

        val pairs = generatePairs(k)

        val meanRank = Array(k) { i -> dataRanked.getMeanOfTreatment(i) }
        val sd = FastMath.sqrt((k * (k + 1)).toDouble() / (6 * n).toDouble())

        val pMatrix = Array(k) { Array(k) { 0.0 } }
        val multiplier = Array(k) { Array(k) { 1 } }

        for (pair in pairs) {
            val stat = FastMath.abs(meanRank[pair.first] - meanRank[pair.second]) / sd
            val pValue = (1.0 - normalDistribution.cumulativeProbability(stat)) * 2.0

            pMatrix[pair.first][pair.second] = pValue
            pMatrix[pair.second][pair.first] = pValue

            // Determine the multiplier.
            multiplier[pair.first][pair.second] = if (meanRank[pair.first] < meanRank[pair.second]) -1 else 1
            multiplier[pair.second][pair.first] = if (meanRank[pair.second] < meanRank[pair.first]) -1 else 1
        }

        return adjustAndCreateResult(pMatrix, multiplier)
    }

    /**
     * Skillings-Mack (pairwise) post-hoc test.
     */
    private fun skillingsMackPostHoc(dataRanked: StatData): Array<Array<TestResult>> {

        val k = dataRanked.getK()
        val pMatrix = Array(k) { Array(k) { 0.0 } }
        val multiplier = Array(k) { Array(k) { 1 } }

        val pairs = generatePairs(k)

        for (pair in pairs) {
            val subsetRanked = dataRanked.getSubset(pair.first, pair.second)
            val pValue = skillingsMack(subsetRanked)

            pMatrix[pair.first][pair.second] = pValue
            pMatrix[pair.second][pair.first] = pValue

            // Determine the multiplier.
            val subsetRanked_ = skillingsMackPrepareData(subsetRanked)
            val meanRank = Array(2) { i -> subsetRanked_.getMeanOfTreatment(i) }
            multiplier[pair.first][pair.second] = if (meanRank[0] < meanRank[1]) -1 else 1
            multiplier[pair.second][pair.first] = if (meanRank[1] < meanRank[0]) -1 else 1
        }

        return adjustAndCreateResult(pMatrix, multiplier)
    }

    /**
     * Corrects the p-values within _pMatrix_ and sets the relation (Smaller / Greater) of the according differences.
     */
    private fun adjustAndCreateResult(
        pMatrix: Array<Array<Double>>,
        multiplier: Array<Array<Int>>
    ): Array<Array<TestResult>> {
        val k = pMatrix.size
        val pMatrixAdj = Shaffer.correct(pMatrix)

        return Array(k) { i ->
            Array(k) { j ->
                if (i != j && pMatrixAdj[i][j] < alpha) {
                    if (multiplier[i][j] < 0) TestResult.Smaller else TestResult.Greater
                } else TestResult.Indifferent
            }
        }
    }
}