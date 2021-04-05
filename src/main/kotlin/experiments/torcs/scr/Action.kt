package experiments.torcs.scr

/**
 * Kotlin translation of the original SCR TORCS implementation.
 *
 * SOURCE: Loiacono, Daniele, Luigi Cardamone, and Pier Luca Lanzi. "Simulated car racing championship: Competition software manual." arXiv preprint arXiv:1304.1672 (2013).
 */
class Action {
    var accelerate = 0.0 // 0..1
    var brake = 0.0 // 0..1
    var clutch = 0.0 // 0..1
    var gear = 0 // -1..6
    var steering = 0.0 // -1..1
    var restartRace = false
    var focus = 360 //ML Desired focus angle in degrees [-90; 90], set to 360 if no focus reading is desired!

    override fun toString(): String {
        limitValues()
        return ("(accel $accelerate) " +
                "(brake $brake) " +
                "(clutch $clutch) " +
                "(gear $gear) " +
                "(steer $steering) " +
                "(meta ${(if (restartRace) 1 else 0)}) " +
                "(focus $focus)")
    }

    fun limitValues() {
        accelerate = accelerate.coerceIn(0.0, 1.0)
        brake = brake.coerceIn(0.0, 1.0)
        clutch = clutch.coerceIn(0.0, 1.0)
        steering = steering.coerceIn(-1.0, 1.0)
        gear = gear.coerceIn(-1, 6)
    }
}