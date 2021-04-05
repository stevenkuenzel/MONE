package settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File


/**
 * Setting manager. Loads and stores all user-settings.
 *
 * @constructor Creates a new instance.
 */
@Serializable
class SettingManager {

    companion object {
        /**
         * The unique instance of SettingManager.
         */
        lateinit var global: SettingManager

        /**
         * Returns the setting with the given name.
         *
         * @param name Name of the setting.
         * @return The setting instance.
         */
        fun get(name: String): Setting {
            return global.get(name)
        }

        /**
         * Creates a default settings file. Saved to the specified JSON-file. Called if no setting file could be found.
         *
         * @param file The target path of the JSON-file.
         * @return A default instance of the SettingManager.
         */
        private fun createDefault(file: File): SettingManager {
            val manager = SettingManager()

            manager.settings.add(Setting("RANDOM_SEED", 0L))


            manager.settings.add(Setting("EVALUATIONS_MAX", 10000))
            manager.settings.add(Setting("POPULATION_SIZE", 100))
            manager.settings.add(Setting("SEDR_SAMPLES_MIN", 3))
            manager.settings.add(Setting("SEDR_SAMPLES_MAX", 15))

            manager.settings.add(
                Setting(
                    "ALGORITHM",
                    "0",
                    "0 = nNEAT, 1 = NEAT-MODS, 2 = NEAT-PS. Multi-selection allowed (separate by comma): 0,1,2"
                )
            )
            manager.settings.add(
                Setting(
                    "PARAMETER_CONTROLLER",
                    "0",
                    "0 = EARPC, 1 = DDYPC, 2 = Default, 3 = Random. Multi-selection allowed (separate by comma): 0,1,2"
                )
            )



            manager.settings.add(
                Setting(
                    "NNEAT_Q_PROCEDURE",
                    "3",
                    "0 = NDR + R2, 1 = R2, 2 = R2 (it), 3 = (NDR + R2, CD). Multi-selection allowed (separate by comma): 0,1,2"
                )
            )
            // Export settings.
            manager.settings.add(Setting("EXPORT_PARETO_FRONT_EVERY_GENERATION", false))
            manager.settings.add(Setting("EXPORT_PARETO_FRONT_AFTER_TERMINATION", true))

            // Concurrency.
            manager.settings.add(Setting("THREADS_MAX", 8)) // TODO.
            manager.settings.add(
                Setting(
                    "EXPERIMENT_EVALUATE_SOLUTIONS_CONCURRENTLY",
                    0,
                    "Determines whether the candidate solutions of a single population are evaluated concurrently. Values: 0 = Experiment default, 1 = Force concurrent, 2 = Force sequential."
                )
            )
            manager.settings.add(
                Setting(
                    "EXPERIMENTS_RUN_ALL_CONCURRENTLY",
                    false,
                    "Determines whether all experiment instances are run concurrently."
                )
            )

            // Experiment settings.
            manager.settings.add(Setting("EXPERIMENT_REPETITIONS", 15))
            manager.settings.add(
                Setting(
                    "EXPERIMENT",
                    "0",
                    "0 = DPB, 1 = FTG, 2 = TORCS. Multi-selection allowed (separate by comma): 0,1,2"
                )
            )
            manager.settings.add(Setting("EXPERIMENT_NOISE", false))


            // Experiment specific.
            manager.settings.add(Setting("DPB_VELOCITIES", true))

            manager.settings.add(Setting("FTG_ROUNDS", 3))
            manager.settings.add(Setting("FTG_FRAMES_PER_ROUND", 3600))
            manager.settings.add(
                Setting(
                    "FTG_OPPONENTS",
                    "DummyBot,DummyBotFooAI",
                    "DummyBot = Strategy 2, DummyBotFooAI = FooAI fallback. Multi-selection allowed (separate by comma)."
                )
            )

            manager.settings.add(Setting("TORCS_SIMTORCS", true))
            manager.settings.add(
                Setting(
                    "TORCS_SIMTORCS_TRACKS",
                    "Brondehach,!Brondehach",
                    "Tracks must be provided in input/tracks directory as XML-file. '!' in front of the track name denotes, that the track turns are inverted. Multi-selection allowed (separate by comma)."
                )
            )
//            manager.settings.add(Setting("TORCS_SIMTORCS_TRACKS", "Brondehach,Forza,G-Track-1,G-Track-3,Wheel-2", "Tracks must be provided in input/tracks directory as XML-file. '!' in front of the track name denotes, that the track turns are inverted. Multi-selection allowed (separate by comma)."))
            manager.settings.add(
                Setting(
                    "TORCS_SIMTORCS_LEVEL_OF_DETAIL",
                    1,
                    "Any value >= 1. Suggestion: Use 1 for training and a higher value, e.g. 5 for validation."
                )
            )

            val json = Json { prettyPrint = true }
            file.writeText(json.encodeToString(manager))

            println("Created a new settings file.")

            return manager
        }

        /**
         * Loads the SettingManager from the disk.
         *
         * @param file The file path of the JSON-file.
         * @return An instance of the SettingManager.
         */
        private fun loadFromFile(file: File): SettingManager {
            return Json.decodeFromString(file.readText())
        }

        /**
         * Checks whether a settings file exists and loads its content. Otherwise a default SettingManager is created.
         *
         * @param file The target file path.
         */
        fun loadOrCreate(file: File) {
            global = if (file.exists()) {
                loadFromFile(file)
            } else {
                createDefault(file)
            }
        }
    }

    /**
     * List of defined settings.
     */
    val settings = mutableListOf<Setting>()

    /**
     * Returns the setting with the given name.
     *
     * @param name Name of the setting.
     * @return The setting instance.
     */
    fun get(name: String): Setting {
        val setting = settings.find { it.name == name }

        if (setting != null) return setting

        throw Exception("Setting $name not registered.")
    }
}