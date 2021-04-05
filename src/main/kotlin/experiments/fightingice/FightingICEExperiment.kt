package experiments.fightingice

import de.stevenkuenzel.xml.XElement
import elements.phenotype.neuralnetworks.NetworkPhenotype
import experiments.Experiment
import experiments.Reference
import experiments.SampleVector
import experiments.fightingice.annbot.ANNBot
import experiments.fightingice.annbot.JointProbabilityDistribution
import experiments.fightingice.annbot.RawInformationLoader
import experiments.fightingice.observation.ObservationCreator
import ftginterface.Fight
import settings.SettingManager
import util.CharacterRoundData
import util.io.PathUtil
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * The FightingICE experiment.
 *
 * @constructor Creates a new instance.
 *
 * @param id The experiment id.
 * @param noise If true, fights start with random player positions.
 */
class FightingICEExperiment(id: Int, noise: Boolean) : Experiment(id, 6, 4, false, noise, true) {

    companion object {
        var jpds: MutableList<JointProbabilityDistribution>? = null

        /**
         * Loads the JPDs or creates those from raw information or novel observations.
         *
         * @return The list of JPDs.
         */
        fun createOrLoadJPDs(): MutableList<JointProbabilityDistribution> {
            if (jpds != null) return jpds!!

            val jpdPath = PathUtil.inputDir + "ANNBot_JPD.xml"
            val rawPath = PathUtil.inputDir + "ANNBot_Raw.xml"


            // Case 1: Create new observation data.
            if (!File(jpdPath).exists() && !File(rawPath).exists()) {
                println("[ANNBot] Creating observation data.")

                val xObservation = ObservationCreator(5000, 1, PathUtil.workingDir + "external/FightingICE").run()

                xObservation.save(rawPath)
            }

            // Case 2: Load existing observation data.
            if (!File(jpdPath).exists()) {
                println("[ANNBot] Processing observation data.")

                jpds = RawInformationLoader.fromXElement(XElement.load(rawPath)!!).createJPDs(10)

                val xJPDs = XElement("JPDCollection")
                jpds!!.forEach { xJPDs.add(it.toXElement()) }

                xJPDs.save(jpdPath)
            } else {
                // Case 3: Load preprocessed JPDs.
                println("[ANNBot] Loading preprocessed JPDs.")

                jpds = mutableListOf()

                XElement.load(jpdPath)!!.getChildren("Action")
                    .forEach { jpds!!.add(JointProbabilityDistribution.fromXElement(it)) }
            }

            return jpds!!
        }
    }

    override val name = "FightingICE"
    override val evaluateEachSolutionOnce = true

    /**
    Number of rounds per fight.
     */
    val numOfRounds = SettingManager.get("FTG_ROUNDS").getValueAsInt()

    /**
    Number of frames per round.
     */
    val framesPerRound = SettingManager.get("FTG_FRAMES_PER_ROUND").getValueAsInt()


    init {
        // Check whether the required files are existing.
        if (!File(PathUtil.workingDir + "external/FightingICE").exists())
        {
            throw Exception("Please ensure that FightingICE is installed in ${PathUtil.workingDir}external/FightingICE.")
        }

        /**
         * Load the opponents to consider a references.
         */
        val opponentNames = SettingManager.get("FTG_OPPONENTS").value.split(",")

        var nextReferenceID = 0

        opponentNames.forEach {
            references.add(Reference(nextReferenceID++, it.trim()))
        }

        createOrLoadJPDs()
    }

    override fun sampleAgainst(phenotype: NetworkPhenotype, reference: Reference): SampleVector {
        // Create the fight.
        val fight = Fight(numOfRounds, framesPerRound, noise, false, PathUtil.workingDir + "external/FightingICE")

        // Create ANNBot.
        val annBot = ANNBot(phenotype, jpds!!)
        fight.setPlayer(0, annBot, "ZEN")

        // Create the opponent.
        when (reference.name) {
            "DummyBot" -> {
                fight.setPlayer(1, DummyBot(false), "ZEN")
            }
            "DummyBotFooAI" -> {
                fight.setPlayer(1, DummyBot(true), "ZEN")
            }
            else -> {
                fight.setPlayer(1, reference.name, "ZEN")
            }
        }

        // Run the fight and return the result.
        val result = fight.run()

        return SampleVector(phenotype.id, reference.id, getFitness(result.result, 0, 1000))
    }

    override fun evaluate(networkPhenotype: NetworkPhenotype, referenceID: Int) {
        val reference = references[referenceID]
        val result = sampleAgainst(networkPhenotype, reference)

        println("Fight against ${reference.name}: ${result.objectives.contentToString()}")
    }

    /**
     * Returns the fitness vector for the result of a Fight.
     *
     * @param data The result of the Fight.
     * @param id The player id.
     * @param ref The reference HP value. Default = 1,000.
     * @return The fitness vector.
     */
    fun getFitness(data: Array<Array<CharacterRoundData>>, id: Int, ref: Int): Array<Double> {
        val fitness = Array(4) { 0.0 }
        val numOfRounds = data[0].size

        for (round in 0 until numOfRounds) {
            fitness[0] += fitnessOpponentDamage(data, id, round, ref)
            fitness[1] += fitnessOwnDamage(data, id, round, ref)
            fitness[2] += fitnessScoreRound(data, id, round)
            fitness[3] += fitnessScoreAttacks(data, id)
        }

        for (index in fitness.indices) {
            fitness[index] = fitness[index] / numOfRounds.toDouble()
        }

        return fitness
    }

    /**
     * f1. See my thesis.
     */
    fun fitnessOpponentDamage(data: Array<Array<CharacterRoundData>>, id: Int, round: Int, ref: Int): Double {
        val dmg = -data[(id + 1) % 2][round].remainingHP

        val ratio = max(0.0, min(1.0, dmg.toDouble() / ref.toDouble()))

        return 1.0 - ratio
    }

    /**
     * f2. See my thesis.
     */
    fun fitnessOwnDamage(data: Array<Array<CharacterRoundData>>, id: Int, round: Int, ref: Int): Double {
        val dmg = -data[id][round].remainingHP

        val ratio = max(0.0, min(1.0, dmg.toDouble() / ref.toDouble()))

        return ratio
    }

    /**
     * f3. See my thesis.
     */
    fun fitnessScoreRound(data: Array<Array<CharacterRoundData>>, id: Int, round: Int): Double {
        val d1 = -data[id][round].remainingHP
        val d2 = -data[(id + 1) % 2][round].remainingHP

        if (d1 == d2) return 0.5
        return if (d1 < d2) 0.0 else 1.0
    }

    /**
     * f4. See my thesis.
     *
     * Note: This fitness value is determined per fight and not per round.
     */
    fun fitnessScoreAttacks(data: Array<Array<CharacterRoundData>>, id: Int): Double {

        var allAttacks = 0
        var successfulAttacks = 0

        for (roundData in data[id]) {
            allAttacks += roundData.offensiveAttacksStarted + roundData.offensiveProjectilesStarted
            successfulAttacks += roundData.offensiveAttacksHit + roundData.offensiveProjectilesHit
        }
        return 1.0 - (successfulAttacks.toDouble() / allAttacks.toDouble())
    }
}