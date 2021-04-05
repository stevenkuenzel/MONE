package settings

import experiments.Experiment

/**
 * Enum of available experiments.
 *
 * @property id The experiment ID.
 * @property create Lambda expression that creates an instance of the respective experiment.
 */
enum class EnumExperiment(val id: Int, private val create: (Int) -> Experiment) {
    DoublePoleBalancing(0, { id ->
        experiments.dpb.DoublePoleBalancing(
            id,
            SettingManager.get("DPB_VELOCITIES").getValueAsBoolean(),
            SettingManager.get("EXPERIMENT_NOISE").getValueAsBoolean()
        )
    }),

    FightingICE(1, { id ->
        experiments.fightingice.FightingICEExperiment(
            id,
            SettingManager.get("EXPERIMENT_NOISE").getValueAsBoolean()
        )
    }),

    TORCS(2, { id ->
        experiments.torcs.TORCSExperiment(
            id, !SettingManager.get("TORCS_SIMTORCS").getValueAsBoolean(),
            SettingManager.get("EXPERIMENT_NOISE").getValueAsBoolean(), true
        )
    }),


    /*
    EXPERIMENTAL / FUTURE WORK. NOT PART OF MY THESIS.
     */

    DTLZ1(3, {id ->
        experiments.testproblems.dtlz.DTLZ1(id)
    }),

    Heightmap(4, {id -> experiments.heightmaps.HeightmapGenerator(id)})
    ;

    /**
     * Returns a copy of the algorithm for the given experiment.
     *
     * @param id The ID of the experiment.
     * @return An instance of the experiment.
     */
    fun getCopyOfExperiment(id : Int): Experiment {
        return create(id)
    }
}