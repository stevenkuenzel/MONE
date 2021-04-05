package experiments.fightingice.observation

import de.stevenkuenzel.xml.XElement
import experiments.fightingice.DummyBot
import ftginterface.Fight
import ftginterface.FightObservation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

/**
 * Creates a certain number of fight observations.
 *
 * @property numOfFights Number of fights.
 * @property numOfRounds Number of rounds per fight.
 * @property fightingICEroot Root directory of the FightingICE environment.
 * @constructor Creates a new instance.
 */
class ObservationCreator(val numOfFights: Int, val numOfRounds: Int = 1, val fightingICEroot: String) {

    /**
     * Runs the observation with the given character (both players).
     *
     * @param character The character to investigate. Default = ZEN.
     * @return An XElement containing the raw observation data.
     */
    fun run(character: String = "ZEN"): XElement {
        val allObservations = mutableListOf<FightObservation>()

        // Run the fights for two reference opponents.
        for (i in 0 until 2) {
            val observations = Collections.synchronizedList(mutableListOf<FightObservation>())

            // Use multiple threads for concurrent simulation.
            val scope = CoroutineScope(Dispatchers.Default)
            runBlocking(scope.coroutineContext) {
                for (j in 0 until numOfFights) {
                    launch {
                        val fight = Fight(numOfRounds, 3600, false, true, fightingICEroot)
                        fight.setPlayer(0, CollectionBot(), character)
                        fight.setPlayer(1, createOpponent(i), "DummyBot$i", character)
                        observations.add(fight.run().observation)

                        println("Finished fight $j.")
                    }
                }
            }

            // Merge all observations per opponent.
            val cumulatedObservation = observations[0]

            for (k in 1 until observations.size) {
                cumulatedObservation.merge(observations[k])
            }

            allObservations.add(cumulatedObservation)
        }

        // Merge all observations (independent of the opponent).
        val finalObservation = allObservations[0];

        for (k in 1 until allObservations.size) {
            finalObservation.merge(allObservations[k])
        }

        // Return the XElement containing the merged raw information.
        return finalObservation.export(0)
    }

    /**
     * Returns an opponent instance.
     *
     * @param index The opponent index. 0 = DummyBot with Strategy 2. 1 = DummyBot with FooAI fallback.
     * @return The opponent instance.
     */
    private fun createOpponent(index: Int): DummyBot {
        return if (index == 0) DummyBot(false) else DummyBot(true)
    }
}