//package experiments.fightingice.annbot
//
//import aiinterface.AIInterface
//import de.stevenkuenzel.xml.XElement
//import elements.phenotype.neuralnetworks.NetworkPhenotype
//import enumerate.Action
//import enumerate.State
//import ftginterface.skills.SkillLoader
//import simulator.Simulator
//import struct.*
//import util.io.PathUtil
//import java.io.File
//import java.lang.Exception
//import java.util.*
//import kotlin.math.abs
//import kotlin.math.max
//import kotlin.math.min
//
//class ANNBotSimple() : AIInterface {
//
//    val DELAY = 15
//
//    var trainingMode = true
//    var runsInVisualEnvironment = false
//
//    lateinit var simulator: Simulator
//
//    lateinit var fightState: FightState
//    var playerNumber = false
//    var fdNow: FrameData? = null
//    var doComputation = false
//    var isCurrentlyControl = false
//
//    lateinit var network: NetworkPhenotype
//    lateinit var networkOutput: Array<Double>
//
//
//    constructor(network: NetworkPhenotype) : this() {
//        trainingMode = true
//
//        this.network = network
//    }
//
//    override fun initialize(gd: GameData?, playerNumber: Boolean): Int {
//        val networkPath = PathUtil.workingDir + "data/aiData/ANN_" + (if (playerNumber) 0 else 1) + ".xml"
//        runsInVisualEnvironment = !trainingMode && File(networkPath).exists()
//
//        simulator = Simulator(gd!!)
//
//        this.playerNumber = playerNumber
//
//        if (runsInVisualEnvironment) {
//            network = NetworkPhenotype.fromXElement(XElement.load(networkPath)!!)
//        }
//
//
//        fightState = FightState(playerNumber)
//
//        return 0
//    }
//
//
//    private fun canProcess(fd: FrameData): Boolean {
//        return !fd.emptyFlag && fd.remainingTimeMilliseconds > 0
//    }
//
//    override fun getInformation(fd: FrameData?, isControl: Boolean) {
//
//        // 14 x -1, dann folgt 0.
//        if (fd!!.framesNumber == -1) return
//
//        val totalFramesToSimulate = DELAY - 1
//
//        fdNow =
//            if (totalFramesToSimulate > 1) {
//                simulator.simulate(fd, playerNumber, null, null, totalFramesToSimulate)
//            } else {
//                fd
//            }
//
//
//        doComputation =
//            canProcess(fdNow!!)
//
//        if (doComputation) {
//            isCurrentlyControl = isControl
//
//            fightState.update(fdNow!!)
//
//            val actionOppReal = fd.getCharacter(!playerNumber).action
//        }
//    }
//
//    val keysToHit = mutableListOf<Key>()
//
//    override fun processing() {
//
//        if (doComputation) {
//            if (keysToHit.isEmpty()) {
//                createOutput()
//            }
//        }
//    }
//
//    override fun input(): Key {
//        if (keysToHit.isEmpty()) return Key()
//
//        return keysToHit.removeAt(0)
//    }
//
//
//    private fun createOutput() {
//        networkOutput = network.update(fightState.getANNinput(25, 24))
//
//        val int1 = mapInt(networkOutput[0], 10)
//        val int2 = mapInt(networkOutput[1], 10)
//        val int3 = mapInt(networkOutput[2], 9) + 1
//        val int4 = mapInt(networkOutput[3], 4)
//
////        val keysToHit = mutableListOf<Key>()
//
//        if (int1 != 0) {
//            keysToHit.add(mapKey(int1))
//        }
//        if (int2 != 0) {
//            keysToHit.add(mapKey(int2))
//        }
//
//        val key = mapKey(int3)
//        mapABC(int4, key)
//
//        keysToHit.add(key)
//    }
//
//    fun mapABC(value: Int, key: Key) {
//        when (value) {
//            1 -> key.A = true
//            2 -> key.B = true
//            3 -> key.C = true
//        }
//    }
//
//    fun mapKey(value: Int): Key {
//        val key = Key()
//
//        when (value) {
//            1 -> {
//                key.L = true
//                key.D = true
//            }
//            2 -> {
//                key.D = true
//            }
//            3 -> {
//                key.R = true
//                key.D = true
//            }
//            4 -> {
//                key.L = true
//            }
//            6 -> {
//                key.R = true
//            }
//            7 -> {
//                key.L = true
//                key.U = true
//            }
//            8 -> {
//                key.U = true
//            }
//            9 -> {
//                key.R = true
//                key.U = true
//            }
//        }
//
//        return key
//    }
//
//    fun mapInt(value: Double, ranges: Int): Int {
//        val rangeSize = 1.0 / ranges.toDouble()
//
//        var index = 0
//        var current = rangeSize
//
//        while (current < 1.0) {
//            if (value <= current) {
//                return index
//            }
//
//            current += rangeSize
//            index++
//        }
//
//        return ranges - 1
//    }
//
//
//    override fun getInformation(fd: FrameData?) {
//        return getInformation(fd, true)
//    }
//    override fun close() {
//    }
//
//    override fun roundEnd(p1Hp: Int, p2Hp: Int, frames: Int) {
//        if (!trainingMode) {
//            val myHp = if (playerNumber) p1Hp else p2Hp
//            val oppHp = if (playerNumber) p2Hp else p1Hp
//
//            val ratio = if (myHp + oppHp != 0) {
//                myHp.toDouble() / (myHp + oppHp).toDouble()
//            } else {
//                0.0
//            }
//
//
//            println("Round end with ${myHp - oppHp}. Ratio = $ratio.")
//        }
//    }
//
//
//    /*
//    * AUX CLASSES
//    */
//
//
//    class FightState(val playerNumber: Boolean) {
//        lateinit var frameData: FrameData
//
//
//        lateinit var myState: CharacterState
//        lateinit var oppState: CharacterState
//
//        fun update(frameData: FrameData) {
//
//            this.frameData = frameData
//
//            myState =
//                CharacterState(
//                    frameData.getCharacter(playerNumber),
//                    playerNumber,
//                    frameData.framesNumber
//
//                )
//            oppState = CharacterState(
//                frameData.getCharacter(!playerNumber),
//                !playerNumber,
//                frameData.framesNumber
//
//            )
//
//            myState.updateInContext(oppState, frameData.projectiles)
//        }
//
//        fun getANNinput(moveXDiff: Int, moveYDiff: Int): Array<Double> {
//            return myState.getInputVector(300.0, 300.0, moveXDiff, moveYDiff)
//        }
//    }
//
//
//    class CharacterState(
//        characterData: CharacterData,
//        val playerNumber: Boolean,
//        val frameNumber: Int
//    ) {
//
//        val charWidth = 40
//        val charHeight = 205
//
//        val ep = characterData.energy
//
//        val front = characterData.isFront
//
//        val left = characterData.left
//        val right = left + charWidth
//        val top = characterData.top
//        val bottom = top + charHeight
//
//        val speedX = characterData.speedX
//        val speedY = characterData.speedY
//
//        val action = characterData.action
//
//
//        val isGrounded = characterData.state == State.STAND || characterData.state == State.CROUCH
//
//
//        var leftPlayer = false
//        var topPlayer = false
//
//
//        var distanceX = 0
//        var distanceY = 0
//        var speedDiffX = 0
//        var speedDiffY = 0
//
//        var probHitMe = 0.0
//        var probHitOpp = 0.0
//
//
//        fun updateInContext(opp: CharacterState, projectiles: Deque<AttackData>) {
//            if (left < opp.left) {
//                leftPlayer = true
//            } else {
//                opp.leftPlayer = true
//            }
//
//            if (top < opp.top) {
//                topPlayer = true
//            } else if (opp.top < top) {
//                opp.topPlayer = true
//            }
//
//            distanceX = left - opp.left
////            if (modData != null) distanceX -= modData.dX
//            distanceY = top - opp.top
////            if (modData != null) distanceY -= modData.dY
//
//            speedDiffX = if (leftPlayer) speedX - opp.speedX else opp.speedX - speedX
////            if (modData != null) speedDiffX -= modData.sX
//            speedDiffY = if (topPlayer) speedY - opp.speedY else opp.speedY - speedY
////            if (modData != null) speedDiffY -= modData.sY
//            opp.speedDiffX = speedDiffX
//            opp.speedDiffY = speedDiffY
//        }
//
//
//        private fun clampInRange(value: Double, vMin: Double, vMax: Double): Double {
//            return min(vMax, max(vMin, value))
//        }
//
//        private fun transferRange(value: Double, min1: Double, max1: Double, min2: Double, max2: Double): Double {
//            val valueIn01 = (value - min1) / (max1 - min1)
//
//            return valueIn01 * (max2 - min2) + min2
//        }
//
//        private fun transferRange01(value: Double, min1: Double, max1: Double): Double {
//            return transferRange(value, min1, max1, 0.0, 1.0)
//        }
//
//
//        private fun transferRangeMinus11(value: Double, min1: Double, max1: Double): Double {
//            return transferRange(value, min1, max1, -1.0, 1.0)
//        }
//
//
//        private fun clipAndTransferRange01(value: Double, vMin: Double, vMax: Double): Double {
//            return transferRange01(clampInRange(value, vMin, vMax), vMin, vMax)
//        }
//
//        private fun clipAndTransferRangeMinus11(value: Double, vMin: Double, vMax: Double): Double {
//            return transferRangeMinus11(clampInRange(value, vMin, vMax), vMin, vMax)
//        }
//
//        fun getInputVector(wMaxD : Double = 300.0, hMaxD : Double = 300.0, moveXDiff : Int, moveYDiff : Int): Array<Double> {
//            val input = mutableListOf<Double>()
//
//            input.add(clipAndTransferRange01(speedDiffX / moveXDiff.toDouble(), -1.0, 1.0))
//            input.add(clipAndTransferRange01(speedDiffY / moveYDiff.toDouble(), -1.0, 1.0))
//            input.add(clipAndTransferRange01(abs(distanceX).toDouble() / wMaxD, 0.0, 1.0))
//            input.add(clipAndTransferRange01(distanceY.toDouble() / hMaxD, -1.0, 1.0))
//
//            return input.toTypedArray()
//        }
//    }
//}