package postprocessing.experiments

/**
 * Determines differnt settings for the experiments.
 *
 * @constructor Create empty Exp settings
 */
class ExpSettings {
    companion object
    {
        /**
         * The minimum fitness F* for each experiment. 0 = DPB, 1 = FTG, 2 = simTORCS.
         */
        val successMinFitnessMap = hashMapOf<String, DoubleArray>()

        init {
            successMinFitnessMap["1"] = doubleArrayOf(0.4, 0.4, 0.0, 0.4)
            successMinFitnessMap["0"] = doubleArrayOf(0.0, 0.05, 0.08)
            successMinFitnessMap["2"] = doubleArrayOf(0.2, 0.1, 0.15)
        }
    }
}