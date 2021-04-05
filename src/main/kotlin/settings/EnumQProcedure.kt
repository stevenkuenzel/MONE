package settings

import sorting.CombinedQProcedure
import sorting.QProcedure
import sorting.impl.CrowdingDistance
import sorting.impl.NondominatedRanking
import sorting.impl.R2Indicator

/**
 * Enum of predefined q-Procedures.
 *
 * @property id The ID of the q-Procedure.
 * @property create Lambda expression that creates an instance of the respective q-Procedure.
 */
enum class EnumQProcedure(val id: Int, private val create: () -> QProcedure) {
    NDR_R2(0, { NondominatedRanking(R2Indicator()) }),
    R2(1, { R2Indicator() }),
    R2it(2, { R2Indicator(true) }),
    NDR_R2__CD(3, { CombinedQProcedure(NondominatedRanking(R2Indicator()), CrowdingDistance()) }),

    /*
    EXPERIMENTAL / FUTURE WORK. NOT PART OF MY THESIS.
     */
    TEST(4, { CombinedQProcedure(NondominatedRanking(R2Indicator()), sorting.impl.RieszSEnergy()) });

    /**
     * Returns a copy of the q-Procedure.
     * @return An instance of the q-Procedure.
     */
    fun getCopyOfQProcedure(): QProcedure {
        return create()
    }
}