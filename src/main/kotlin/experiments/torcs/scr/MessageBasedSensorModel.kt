package experiments.torcs.scr

/**
 * Kotlin translation of the original SCR TORCS implementation.
 *
 * SOURCE: Loiacono, Daniele, Luigi Cardamone, and Pier Luca Lanzi. "Simulated car racing championship: Competition software manual." arXiv preprint arXiv:1304.1672 (2013).
 */
class MessageBasedSensorModel(val message : MessageParser) : SensorModel {
    constructor(strMessage : String) : this(MessageParser(strMessage))

    override fun getSpeed(): Double {
        return message.getReading("speedX") as Double
    }

    override fun getAngleToTrackAxis(): Double {
        return message.getReading("angle") as Double
    }

    override fun getTrackEdgeSensors(): DoubleArray {
        return message.getReading("track") as DoubleArray
    }

    override fun getFocusSensors(): DoubleArray {
        return message.getReading("focus") as DoubleArray
    }

    override fun getTrackPosition(): Double {
        return message.getReading("trackPos") as Double
    }

    override fun getGear(): Int {
        return (message.getReading("gear") as Double).toInt()
    }

    override fun getOpponentSensors(): DoubleArray {
        return message.getReading("opponents") as DoubleArray
    }

    override fun getRacePosition(): Int {
        return (message.getReading("racePos") as Double).toInt()
    }

    override fun getLateralSpeed(): Double {
        return message.getReading("speedY") as Double
    }

    override fun getCurrentLapTime(): Double {
        return message.getReading("curLapTime") as Double
    }

    override fun getDamage(): Double {
        return message.getReading("damage") as Double
    }

    override fun getDistanceFromStartLine(): Double {
        return message.getReading("distFromStart") as Double
    }

    override fun getDistanceRaced(): Double {
        return message.getReading("distRaced") as Double
    }

    override fun getFuelLevel(): Double {
        return message.getReading("fuel") as Double
    }

    override fun getLastLapTime(): Double {
        return message.getReading("lastLapTime") as Double
    }

    override fun getRPM(): Double {
        return message.getReading("rpm") as Double
    }

    override fun getWheelSpinVelocity(): DoubleArray {
        return message.getReading("wheelSpinVel") as DoubleArray
    }

    override fun getZSpeed(): Double {
        return message.getReading("speedZ") as Double
    }

    override fun getZ(): Double {
        return message.getReading("z") as Double
    }

    override fun getMessage(): String {
        return message.message
    }
}