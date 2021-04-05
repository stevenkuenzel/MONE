package experiments.fightingice.annbot

import enumerate.Action
import fighting.Motion
import simulator.SimFighting
import simulator.Simulator
import struct.FrameData
import struct.GameData
import java.util.*
import kotlin.math.abs

/**
 * An ANNBot specific simulator that stores the distances between the opponents after each simulated frame.
 *
 */
class ANNBotSimulator(val refToGameData: GameData) : Simulator(refToGameData) {

    lateinit var distanceInformation : Array<Array<Int>>

    override fun simulate(
        frameData: FrameData?,
        playerNumber: Boolean,
        myAct: Deque<Action>?,
        oppAct: Deque<Action>?,
        simulationLimit: Int
    ): FrameData {
        distanceInformation = Array(simulationLimit) { Array(2) { 0 } }

        val tempMotionList = ArrayList<ArrayList<Motion>>(2)
        tempMotionList.add(refToGameData.getMotion(true))
        tempMotionList.add(refToGameData.getMotion(false))

        val tempActionList = ArrayList<Deque<Action>?>(2)
        tempActionList.add(null)
        tempActionList.add(null)

        val simFighting = SimFighting()
        simFighting.initialize(tempMotionList, tempActionList, FrameData(frameData), playerNumber)

        var nowFrame = frameData!!.framesNumber

        // Create the distance array.
        for (i in 0 until simulationLimit)
        {
            simFighting.processingFight(nowFrame++)

            val me = simFighting.characters[if (playerNumber) 0 else 1]!!
            val opp = simFighting.characters[if (playerNumber) 1 else 0]!!

            val dX = abs(me.x - opp.x)
            val dY = me.y - opp.y

            distanceInformation[i][0] = dX
            distanceInformation[i][1] = dY
        }

        return simFighting.createFrameData(nowFrame, frameData.round)
    }
}
