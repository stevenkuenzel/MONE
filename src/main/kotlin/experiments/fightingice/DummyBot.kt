package experiments.fightingice

import aiinterface.AIInterface
import aiinterface.CommandCenter
import enumerate.Action
import enumerate.State
import fighting.Character
import manager.GameManager
import struct.FrameData
import struct.GameData
import struct.Key
import util.random.RandomProvider
import kotlin.math.abs

/**
 * DummyBot. Applied for training and data collection.
 *
 * @property fooAIfallback If true, the bot follows the scheme of FooAI's fallback procedure.
 * @constructor Creates a new instance.
 */
class DummyBot(val fooAIfallback: Boolean) : AIInterface {
    /**
     * Random number generator.
     */
    val random = RandomProvider.create()

    // Necessary information.
    val commandCenter = CommandCenter()
    var nextKey = Key()
    var gameData: GameData? = null
    var gameManager: GameManager? = null
    var frameData: FrameData = FrameData()
    var wMax = 0.0
    var hMax = 0.0
    var playerNumber = false
    var doProcess = true
    var isCurrentlyControl = true
    var myCharacter: Character? = null
    var oppCharacter: Character? = null


    override fun initialize(gd: GameData?, playerNumber: Boolean): Int {
        gameData = gd!!
        gameManager = gd.gameManager!!
        wMax = gd.stageWidth.toDouble()
        hMax = gd.stageHeight.toDouble()
        this.playerNumber = playerNumber

        return 0
    }

    override fun getInformation(fd: FrameData?, isControl: Boolean) {
        frameData = fd!!
        isCurrentlyControl = isControl
        commandCenter.setFrameData(fd, playerNumber)

        doProcess = !fd.emptyFlag && fd.remainingTimeMilliseconds > 0

        myCharacter = gameManager!!.fighting!!.getCharacter(playerNumber)
        oppCharacter = gameManager!!.fighting!!.getCharacter(!playerNumber)
    }

    override fun getInformation(fd: FrameData?) {
        getInformation(fd, true)
    }


    override fun processing() {
        if (doProcess) {
            if (this.commandCenter.skillFlag)
                nextKey = this.commandCenter.skillKey
            else {

                if (isCurrentlyControl) {
                    nextKey.empty()
                    commandCenter.skillCancel()

                    // Determine the next skill.
                    if (fooAIfallback) {
                        simpleFooAI()
                    } else {
                        val action = simpleANNBot()
                        commandCenter.commandCall(action.name)
                    }
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



    var lastWasDash = false

    /**
     * Strategy 2. See my thesis.
     */
    private fun simpleANNBot(): Action {
        return if (lastWasDash) {
            lastWasDash = false
            Action.STAND_B
        } else {
            lastWasDash = true
            Action.DASH
        }
    }

    /**
     * FooAI fallback procedure. We provide perfect information to it to save simulation effort.
     *
     * SOURCE: http://www.ice.ci.ritsumei.ac.jp/~ftgaic/Downloadfiles/2017Competition.zip
     */
    private fun simpleFooAI() {
        val my = myCharacter!!
        val opp = oppCharacter!!

        val myX = my.x
        val oppX = opp.x
        val energy = my.energy
        val xDifference = myX - oppX
        val distance = abs(xDifference)

        if (opp.energy >= 300 && my.hp - opp.hp <= 300) {
            commandCenter.commandCall("FOR_JUMP _B B B")
        } else if (my.state != State.AIR && my.state != State.DOWN) { //if not in air
            if (distance > 150) {
                commandCenter.commandCall("FOR_JUMP") //If its too far, then jump to get closer fast
            } else if (energy >= 300) {
                commandCenter.commandCall("STAND_D_DF_FC") //High energy projectile
            } else if (distance > 100 && energy >= 50) {
                commandCenter.commandCall("STAND_D_DB_BB") //Perform a slide kick
            } else if (opp.state == State.AIR)
            //if enemy on Air
            {
                commandCenter.commandCall("STAND_F_D_DFA") //Perform a big punch
            } else {
                commandCenter.commandCall("B") //Perform a kick in all other cases, introduces randomness
            }
        } else if (distance <= 150 && (my.state == State.AIR || my.state == State.DOWN)
            && (gameData!!.stageWidth - myX >= 200 || xDifference > 0)
            && (myX >= 200 || xDifference < 0)
        ) { //Conditions to handle game corners
            if (energy >= 5) {
                commandCenter.commandCall("AIR_DB") // Perform air down kick when in air
            } else {
                commandCenter.commandCall("B") //Perform a kick in all other cases, introduces randomness
            }
        } else {
            commandCenter.commandCall("B") //Perform a kick in all other cases, introduces randomness
        }// if the opp has 300 of energy, it is dangerous, so better jump!!
        // if the health difference is high we are dominating so we are fearless :)
    }
}