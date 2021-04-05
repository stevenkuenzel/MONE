package experiments.fightingice.observation

import aiinterface.AIInterface
import aiinterface.CommandCenter
import enumerate.Action
import enumerate.State
import fighting.Character
import ftginterface.skills.SkillLoader
import manager.GameManager
import struct.FrameData
import struct.GameData
import struct.Key
import util.random.RandomProvider
import kotlin.math.abs

/**
 * CollectionBot. Applied for randomly committing skills and checking their respective success-rates and most promising distance vectors.
 *
 * @constructor Creates a new instance.
 */
class CollectionBot : AIInterface {
    companion object {
        val ACTIONS_MOVE = arrayOf(
            Action.BACK_JUMP,
            Action.JUMP,
            Action.FOR_JUMP,
            Action.BACK_STEP,
            Action.FORWARD_WALK,
            Action.DASH,
            Action.CROUCH
        )
    }

    /**
     * Random number generator.
     */
    val random = RandomProvider.create()

    // Necessary information.
    var myCharacter: Character? = null
    var oppCharacter: Character? = null

    var nextKey = Key()
    var gameData: GameData? = null
    var frameData: FrameData = FrameData()
    var gameManager: GameManager? = null
    var playerNumber: Boolean = false

    var doProcess = true
    var isCurrentlyControl = true


    /**
     * Provides information about skills.
     */
    val skillLoader = SkillLoader.getSkillLoaderFor("ZEN")

    /**
     * CommandCenter for committing skills.
     */
    private val commandCenter = CommandCenter()

    override fun initialize(gd: GameData?, playerNumber: Boolean): Int {
        gameData = gd!!
        gameManager = gd.gameManager!!
        this.playerNumber = playerNumber

        return 0
    }

    override fun getInformation(fd: FrameData?) {
        getInformation(fd, true)
    }

    override fun getInformation(fd: FrameData?, isControl: Boolean) {
        myCharacter = gameManager!!.fighting!!.getCharacter(playerNumber)
        oppCharacter = gameManager!!.fighting!!.getCharacter(!playerNumber)

        frameData = fd!!
        isCurrentlyControl = isControl
        commandCenter.setFrameData(fd, playerNumber)

        doProcess = !fd.emptyFlag && fd.remainingTimeMilliseconds > 0
    }


    override fun processing() {
        if (doProcess) {
            if (this.commandCenter.skillFlag)
                nextKey = this.commandCenter.skillKey
            else {

                if (isCurrentlyControl) {
                    nextKey.empty()
                    commandCenter.skillCancel()

                    val action = selectNextAction()
                    commandCenter.commandCall(action.name)
                }
            }
        }
    }

    override fun input(): Key {
        return nextKey
    }

    override fun close() {
    }

    override fun roundEnd(p1Hp: Int, p2Hp: Int, frames: Int) {
    }

    /**
     * Selects the next skill to commit.
     *
     * @return The next skill to commit.
     */
    private fun selectNextAction(): Action {
        val air = if (myCharacter != null) {
            myCharacter!!.state == State.AIR

        } else {
            false
        }

        if (myCharacter != null) {
            // Apply EP and HP cheats.
            myCharacter!!.energy = 300
            oppCharacter!!.hp = myCharacter!!.hp - 300
        }

        // Determine whether to move or to attack.
        val value = random.nextDouble()

        return if (!air && value < 0.7) {
            ACTIONS_MOVE.random()
        } else {
            (if (air) skillLoader.combatAir else skillLoader.combatGround).random().action
        }
    }
}