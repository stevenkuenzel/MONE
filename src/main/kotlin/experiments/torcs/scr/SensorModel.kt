package experiments.torcs.scr

/**
 * Kotlin translation of the original SCR TORCS implementation.
 *
 * SOURCE: Loiacono, Daniele, Luigi Cardamone, and Pier Luca Lanzi. "Simulated car racing championship: Competition software manual." arXiv preprint arXiv:1304.1672 (2013).
 */
interface SensorModel {

    // basic information about your car and the track (you probably should take care of these somehow)
    fun getSpeed(): Double

    fun getAngleToTrackAxis(): Double

    fun getTrackEdgeSensors(): DoubleArray

    fun getFocusSensors(): DoubleArray //ML

    fun getTrackPosition(): Double

    fun getGear(): Int

    // basic information about other cars (only useful for multi-car races)
    fun getOpponentSensors(): DoubleArray

    fun getRacePosition(): Int

    // additional information (use if you need)
    fun getLateralSpeed(): Double

    fun getCurrentLapTime(): Double

    fun getDamage(): Double

    fun getDistanceFromStartLine(): Double

    fun getDistanceRaced(): Double

    fun getFuelLevel(): Double

    fun getLastLapTime(): Double

    fun getRPM(): Double

    fun getWheelSpinVelocity(): DoubleArray

    fun getZSpeed(): Double

    fun getZ(): Double

    fun getMessage(): String
}