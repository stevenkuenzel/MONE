package experiments.fightingice.annbot

import aiinterface.AIInterface
import aiinterface.CommandCenter
import de.stevenkuenzel.xml.XElement
import elements.phenotype.neuralnetworks.NetworkPhenotype
import enumerate.Action
import enumerate.State
import experiments.fightingice.FightingICEExperiment
import ftginterface.skills.SkillLoader
import ftginterface.skills.SkillLocation
import simulator.Simulator
import struct.*
import util.io.PathUtil
import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * ANNBot. A neural network controlled bot for FightingICE:
 *
 * @constructor Creates an instance of ANNBot.
 */
open class ANNBot() : AIInterface {
    companion object {
        // Constants.

        val POS_LEFT_MIN = 0
        val POS_LEFT_MAX = 920
        val MARGIN_DANGER = 20
        val DELAY = 15
        var ESTIMATED_SKILL_VALUE_MIN = 0.1
        val MOVE_SPEED_DIFFERENCE_THRESHOLD = 0.05
    }

    /**
     * Stores the JPDs for skill-value determination.
     */
    lateinit var jpds: List<JointProbabilityDistribution>

    /**
     * Training mode constructor. Never called by the 'real' Fighting Game AI Competition.
     */
    constructor(network: NetworkPhenotype, jpds: List<JointProbabilityDistribution>) : this() {
        trainingMode = true

        this.network = network
        this.jpds = jpds
    }

    /**
     * True, if ANNBot is run in training mode. Otherwise: 'Real' FightingICE or evaluation.
     */
    var trainingMode = false

    /**
    True, if ANNBot is run in the 'real' Fighting Game AI Competition.
     */
    var runsInVisualEnvironment = false

    /**
     * Simulator for forecasting the next 14 frames.
     */
    lateinit var simulator: Simulator

    /**
     * Simulator for determining the attack-skill values.
     */
    lateinit var simulatorDistance: ANNBotSimulator

    /**
     * CommandCenter for committing skills.
     */
    private val commandCenter = CommandCenter()

    /**
     * Key committed by ANNBot in the next possible Frame.
     */
    private var nextKey = Key()

    /**
     * Provides information about skills.
     */
    lateinit var skillLoader: SkillLoader

    /**
     * List of skills for which a JPD is available.
     */
    val annbotSkills = mutableListOf<ANNBotSkill>()

    /**
     * Current game state.
     */
    lateinit var fightState: FightState

    /**
     * Current frame data. FightingICE specific.
     */
    var fdNow: FrameData? = null

    /**
     * Player number. True = P1, False = P2.
     */
    var playerNumber = false

    // Constants about the movement speeds.
    var moveXmin = 0
    var moveYmin = 0
    var moveXdiff = 0
    var moveYdiff = 0
    var moveDiffMax = 0.0

    // Can the ANNBot commit a skill?
    var doComputation = false
    var isCurrentlyControl = false

    // Determination of dangerous situations.
    var cornerCount = 0
    var danger = false

    /**
     * The neural network controller.
     */
    lateinit var network: NetworkPhenotype

    /**
     * Output of the neural network.
     */
    lateinit var networkOutput: Array<Double>

    /**
     * Previously suggested attacks.
     */
    var suggestedAttacks = Array<Pair<Action, Double>?>(2) { null }

    override fun initialize(gd: GameData?, playerNumber: Boolean): Int {
        // Try to find the neural network as XML-file. If it exists, assume that ANNBot is run in the visual environment of FightingICE.
        val networkPath = PathUtil.workingDir + "data/aiData/ANN_" + (if (playerNumber) 0 else 1) + ".xml"
        runsInVisualEnvironment = !trainingMode && File(networkPath).exists()

        simulator = Simulator(gd!!)
        simulatorDistance = ANNBotSimulator(gd)

        this.playerNumber = playerNumber


        if (runsInVisualEnvironment) {
            // Load the network and JPDs from the disk.
            network = NetworkPhenotype.fromXElement(XElement.load(networkPath)!!)

            println("[ANNBot] Loading preprocessed JPDs.")
            val jpdPath = de.stevenkuenzel.util.PathUtil.workingDir + "data/aiData/ANNBot_JPD.xml"
            val jpds_ = mutableListOf<JointProbabilityDistribution>()

            if (!File(jpdPath).exists()) {
                throw Exception("The file 'aiData/ANNBot_JPD.xml' is missing.")
            }

            XElement.load(jpdPath)!!.getChildren("Action")
                .forEach { jpds_.add(JointProbabilityDistribution.fromXElement(it)) }

            jpds = jpds_

            // Initialize the skill loader, if necessary.
            if (!SkillLoader.isInitialized) {
                SkillLoader.initialize(PathUtil.workingDir)
            }
        }

        // Get the skill loader for the selected character.
        skillLoader = SkillLoader.getSkillLoaderFor(gd.getCharacterName(playerNumber).toUpperCase())

        // Link the skills and JPDs.
        skillLoader.skills.forEach { skill ->
            val match = jpds.find { jpd -> jpd.action == skill.action }

            if (match != null) {
                annbotSkills.add(ANNBotSkill(skill, match))
            }
        }

        // Extract the movement skills' speed information.
        determineMovementSpeedInformation()

        // Create an empty game state.
        fightState = FightState(playerNumber)

        return 0
    }

    override fun getInformation(fd: FrameData?) {
        return getInformation(fd, true)
    }

    override fun getInformation(fd: FrameData?, isControl: Boolean) {

        // Ignore the first 14 frames of a round. Do nothing.
        if (fd!!.framesNumber == -1) return

        // Simulate the next 14 frames.
        val totalFramesToSimulate = DELAY - 1

        fdNow =
            if (totalFramesToSimulate > 1) {
                simulator.simulate(fd, playerNumber, null, null, totalFramesToSimulate)
            } else {
                fd
            }

        // Update the CommandCenter.
        commandCenter.setFrameData(fd, playerNumber)

        // Can ANNBot commit a skill?
        doComputation =
            canProcess(fdNow!!)

        if (doComputation) {
            isCurrentlyControl = isControl

            // Update the game state.
            fightState.update(fdNow!!)


            // Determine whether a dangerous situation is present.
            // Check if player is trapped in a corner.
            val isInCorner =
                (fightState.myState.leftPlayer && fightState.myState.left < POS_LEFT_MIN + MARGIN_DANGER) ||
                        (!fightState.myState.leftPlayer && fightState.myState.left > POS_LEFT_MAX - MARGIN_DANGER)

            if (isInCorner) cornerCount++ else cornerCount = 0
            val dangerTrapped = cornerCount >= 45

            // Check if opponent starts a fireball.
            val actionOppReal = fd.getCharacter(!playerNumber).action
            val dangerFireball =
                (actionOppReal != null && actionOppReal == Action.STAND_D_DF_FC) || (fightState.oppState.action != null && fightState.oppState.action == Action.STAND_D_DF_FC)

            // Enter the danger routine, when possible.
            danger = dangerFireball || dangerTrapped
        }
    }


    override fun processing() {
        if (danger) {
            // Danger routine.
            clearCommandCenter()
            return
        }

        // Default.
        if (doComputation) {
            if (commandCenter.skillFlag) {
                // Commit next key.
                nextKey = this.commandCenter.skillKey
            } else {
                if (isCurrentlyControl) {
                    // Determine next skill.
                    clearCommandCenter()
                    createOutput()

                    val action = selectSkill()
                    call(action)
                }
            }
        }
    }

    override fun input(): Key {
        if (danger) {
            // Danger routine.
            val dangerKey = Key().also { it.U = true }

            if (fightState.myState.front) {
                dangerKey.R = true
            } else {
                dangerKey.L = true
            }

            return dangerKey
        }

        return nextKey
    }

    override fun close() {
    }

    override fun roundEnd(p1Hp: Int, p2Hp: Int, frames: Int) {
        if (!trainingMode) {
            // Show the final score, if not in training.
            val myHp = if (playerNumber) p1Hp else p2Hp
            val oppHp = if (playerNumber) p2Hp else p1Hp

            val ratio = if (myHp + oppHp != 0) {
                myHp.toDouble() / (myHp + oppHp).toDouble()
            } else {
                0.0
            }

            println("Round end with ${myHp - oppHp}. Ratio = $ratio.")
        }
    }

    private fun canProcess(fd: FrameData): Boolean {
        return !fd.emptyFlag && fd.remainingTimeMilliseconds > 0
    }

    private fun clearCommandCenter() {
        nextKey.empty()
        commandCenter.skillCancel()
    }

    /**
     * Determines the min. and max. movement speeds.     *
     */
    private fun determineMovementSpeedInformation() {
        val moveXsorted = skillLoader.movementSkills.sortedBy { it.speedX }
        val moveYsorted = skillLoader.movementSkills.sortedBy { it.speedY }

        moveXmin = moveXsorted[0].speedX
        val moveXmax = moveXsorted[moveXsorted.size - 1].speedX
        moveXdiff = moveXmax - moveXmin

        moveYmin = moveYsorted[0].speedY
        val moveYmax = moveYsorted[moveYsorted.size - 1].speedY
        moveYdiff = moveYmax - moveYmin

        moveDiffMax = (moveXdiff * moveXdiff + moveYdiff * moveYdiff).toDouble()
    }

    /**
     * Create the output vector of the neural network.
     *
     */
    private fun createOutput() {
        // Determine the next two possible attacks.
        suggestedAttacks = suggestAttacks()

        // Create the network input vector.
        val fightStateInput = fightState.getANNinput(moveXdiff, moveYdiff)
        val networkInput = Array(suggestedAttacks.size + fightStateInput.size) { 0.0 }

        var inputIndex = 0

        // Attacks.
        for (index in suggestedAttacks.indices) {
            networkInput[inputIndex++] = if (suggestedAttacks[index] != null) {
                (suggestedAttacks[index]!!.second - ESTIMATED_SKILL_VALUE_MIN) / (1.0 - ESTIMATED_SKILL_VALUE_MIN)
            } else {
                -1.0
            }
        }

        // Other input.
        for (index in fightStateInput.indices) {
            networkInput[inputIndex++] = fightStateInput[index]
        }

        // Activate the network.
        networkOutput = network.update(networkInput)
    }

    /**
     * Suggest the fastest and strongest attacks to ANNBot.
     *
     * @return Up to two suggested attacks.
     */
    private fun suggestAttacks(): Array<Pair<Action, Double>?> {
        val location = if (fightState.myState.isGrounded) SkillLocation.OnGround else SkillLocation.InAir

        // Filter the skills for availability.
        val availableSkills =
            annbotSkills.filter { it.skill.attackLocation == location && it.skill.energyRequired <= fightState.myState.ep }
                .map { SkillHandle(it) }

        if (availableSkills.isEmpty()) return Array(2) { null }

        // Determine the number of frames to simulate ahead.
        val maxSim = availableSkills.maxByOrNull { it.annBotSkill.skill.startUp }!!.annBotSkill.skill.startUp + 2

        // Retrieve the frame-wise distance information.
        simulatorDistance.simulate(fdNow, playerNumber, null, null, maxSim)
        val distanceInformation = simulatorDistance.distanceInformation

        // Approximate the values of the skills.
        for (skillWrapper in availableSkills) {
            val index = min(
                skillWrapper.annBotSkill.skill.startUp + skillWrapper.annBotSkill.skill.keyCount - 1,
                distanceInformation.size - 1
            )
            val dX = distanceInformation[index][0]
            val dY = distanceInformation[index][1]

            skillWrapper.estimatedValue = skillWrapper.annBotSkill.approximate(dX, dY)
            skillWrapper.estimatedDamage = skillWrapper.estimatedValue * skillWrapper.damage
            skillWrapper.estimatedTime = skillWrapper.estimatedValue / skillWrapper.numOfFrames
        }

        // Filter the skills for the subset that has a certain minimum chance of being successful.
        val reasonableSkills = availableSkills.filter { it.estimatedValue > ESTIMATED_SKILL_VALUE_MIN }

        if (reasonableSkills.isEmpty()) return Array(2) { null }

        if (reasonableSkills.size > 1) {
            val skillBestDamage = reasonableSkills.maxByOrNull { it.estimatedDamage }!!
            val skillBestTime = reasonableSkills.maxByOrNull { it.estimatedTime }!!

            // Return the fastest and strongest skill.
            return arrayOf(skillBestDamage.getActionValuePair(), skillBestTime.getActionValuePair())
        }

        // Return the single remaining skill.
        return arrayOf(reasonableSkills[0].getActionValuePair(), reasonableSkills[0].getActionValuePair())
    }

    /**
     * Selects the next skill based on the network output.
     *
     * @return The next skill's Action.
     */
    private fun selectSkill(): Action? {
        val moveAction = selectMove(networkOutput[0], networkOutput[1])

        if (moveAction != null) return moveAction

        // This includes idle. If both are null.
        return if (networkOutput[2] >= networkOutput[3]) {
            suggestedAttacks[0]?.first
        } else {
            suggestedAttacks[1]?.first
        }
    }

    /**
     * Select a movement skill based on the target speed vector.
     *
     * @param x Speed in x-direction.
     * @param y Speed in y-direction.
     * @return The movement skill or null, if an attack is to be selected.
     */
    private fun selectMove(x: Double, y: Double): Action? {

        val tX = moveXmin + (moveXdiff.toDouble() * x).toInt()
        val tY = moveYmin + (moveYdiff.toDouble() * (1.0 - y)).toInt()

        var action: Action? = null
        var dMin = Int.MAX_VALUE

        // Determine the movement skill with the minimum Euclidean distance of the speed vectors.
        for (move in skillLoader.movementSkills) {
            val dx = move.speedX - tX
            val dy = move.speedY - tY

            val distance = dx * dx + dy * dy

            if (distance < dMin) {
                dMin = distance
                action = move.action
            }
        }

        // Found a movement skill.
        if (dMin.toDouble() / moveDiffMax <= MOVE_SPEED_DIFFERENCE_THRESHOLD) {
            return action
        }

        // Select an attack skill.
        return null
    }

    /**
     * Calls an action via the CommandCenter.
     *
     * @param action The action to call.
     */
    private fun call(action: Action?) {
        if (action != null) {

            commandCenter.commandCall(action.name)

        } // else: Idle
    }

    /*
     * AUX CLASSES
     */

    /**
     * Contains the game state at a certain time _t_ from the perspective of either player.
     *
     * @property playerNumber The player.
     * @constructor Creates a new instance.
     */
    class FightState(val playerNumber: Boolean) {
        lateinit var frameData: FrameData
        lateinit var myState: CharacterState
        lateinit var oppState: CharacterState

        fun update(frameData: FrameData) {

            this.frameData = frameData

            myState =
                CharacterState(
                    frameData.getCharacter(playerNumber)
                )
            oppState = CharacterState(
                frameData.getCharacter(!playerNumber)
            )

            myState.updateInContext(oppState)
        }

        fun getANNinput(moveXDiff: Int, moveYDiff: Int): Array<Double> {
            return myState.getInputVector(300.0, 300.0, moveXDiff, moveYDiff)
        }
    }


    /**
     * Contains information about a player.
     *
     * @constructor Creates a new instance.
     *
     * @param characterData The FightingICE character data.
     */
    class CharacterState(
        characterData: CharacterData
    ) {
        // Different Information:
        val ep = characterData.energy

        val front = characterData.isFront
        val left = characterData.left
        val top = characterData.top

        val speedX = characterData.speedX
        val speedY = characterData.speedY

        val action = characterData.action

        val isGrounded = characterData.state == State.STAND || characterData.state == State.CROUCH

        var leftPlayer = false
        var topPlayer = false

        var distanceX = 0
        var distanceY = 0
        var speedDiffX = 0
        var speedDiffY = 0

        /**
         * Update information in relation to the other player.
         *
         * @param opp The other player.
         */
        fun updateInContext(opp: CharacterState) {
            if (left < opp.left) {
                leftPlayer = true
            } else {
                opp.leftPlayer = true
            }

            if (top < opp.top) {
                topPlayer = true
            } else if (opp.top < top) {
                opp.topPlayer = true
            }

            distanceX = left - opp.left
            distanceY = top - opp.top
            speedDiffX = if (leftPlayer) speedX - opp.speedX else opp.speedX - speedX
            speedDiffY = if (topPlayer) speedY - opp.speedY else opp.speedY - speedY
            opp.speedDiffX = speedDiffX
            opp.speedDiffY = speedDiffY
        }

        /**
         * Returns the input vector for the neural network.
         *
         * @param wMaxD Considered distance X.
         * @param hMaxD Considered distance Y.
         * @param moveXDiff Max. movement speed difference X.
         * @param moveYDiff Max. movement speed difference Y.
         * @return The input vector.
         */
        fun getInputVector(
            wMaxD: Double = 300.0,
            hMaxD: Double = 300.0,
            moveXDiff: Int,
            moveYDiff: Int
        ): Array<Double> {
            val input = mutableListOf<Double>()

            input.add(clipAndTransferRange01(speedDiffX / moveXDiff.toDouble(), -1.0, 1.0))
            input.add(clipAndTransferRange01(speedDiffY / moveYDiff.toDouble(), -1.0, 1.0))
            input.add(clipAndTransferRange01(abs(distanceX).toDouble() / wMaxD, 0.0, 1.0))
            input.add(clipAndTransferRange01(distanceY.toDouble() / hMaxD, -1.0, 1.0))

            return input.toTypedArray()
        }

        // Methods for normalizing and shifting input values.

        private fun clampInRange(value: Double, vMin: Double, vMax: Double): Double {
            return min(vMax, max(vMin, value))
        }

        private fun transferRange(value: Double, min1: Double, max1: Double, min2: Double, max2: Double): Double {
            val valueIn01 = (value - min1) / (max1 - min1)

            return valueIn01 * (max2 - min2) + min2
        }

        private fun transferRange01(value: Double, min1: Double, max1: Double): Double {
            return transferRange(value, min1, max1, 0.0, 1.0)
        }


        private fun transferRangeMinus11(value: Double, min1: Double, max1: Double): Double {
            return transferRange(value, min1, max1, -1.0, 1.0)
        }


        private fun clipAndTransferRange01(value: Double, vMin: Double, vMax: Double): Double {
            return transferRange01(clampInRange(value, vMin, vMax), vMin, vMax)
        }

//        private fun clipAndTransferRangeMinus11(value: Double, vMin: Double, vMax: Double): Double {
//            return transferRangeMinus11(clampInRange(value, vMin, vMax), vMin, vMax)
//        }
    }

    /**
     * Assigns a value to a skill.
     *
     * @property annBotSkill The skill.
     * @constructor Creates a new instance.
     */
    data class SkillHandle(val annBotSkill: ANNBotSkill) {
        var damage = annBotSkill.skill.damage.toDouble()
        var numOfFrames = annBotSkill.skill.numOfFrames.toDouble()

        var estimatedValue = 0.0
        var estimatedDamage = 0.0
        var estimatedTime = 0.0

        fun getActionValuePair(): Pair<Action, Double> {
            return Pair(annBotSkill.skill.action, estimatedValue)
        }

        override fun toString(): String {
            return annBotSkill.skill.name + " ($estimatedValue / $estimatedDamage / $estimatedTime)"
        }
    }

}