package experiments.torcs.scr

/**
 * Kotlin translation of the original SCR TORCS implementation.
 *
 * SOURCE: Loiacono, Daniele, Luigi Cardamone, and Pier Luca Lanzi. "Simulated car racing championship: Competition software manual." arXiv preprint arXiv:1304.1672 (2013).
 */
enum class ControllerStage {
    WARMUP,
    QUALIFYING,
    RACE,
    UNKNOWN;

    fun fromInt(value: Int): ControllerStage {
        return when (value) {
            0 -> WARMUP
            1 -> QUALIFYING
            2 -> RACE
            else -> UNKNOWN
        }
    }
}