package experiments.torcs.scr

/**
 * Kotlin translation of the original SCR TORCS implementation.
 *
 * SOURCE: Loiacono, Daniele, Luigi Cardamone, and Pier Luca Lanzi. "Simulated car racing championship: Competition software manual." arXiv preprint arXiv:1304.1672 (2013).
 */
abstract class Controller {
    var stage = ControllerStage.UNKNOWN
    var trackName = ""

    open fun initAngles(): FloatArray {
        return FloatArray(19) { i -> (-90 + i * 10).toFloat() }
    }

    abstract fun control(sensors: SensorModel?): Action?

    abstract fun reset() // called at the beginning of each new trial


    abstract fun shutdown()
}