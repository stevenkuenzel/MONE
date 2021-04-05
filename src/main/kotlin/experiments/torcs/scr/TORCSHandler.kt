package experiments.torcs.scr

import experiments.torcs.annracer.ANNRacer

/**
 * Kotlin adaoption (to ANNRacer) of the original SCR TORCS implementation. Only minor changes.
 *
 * SOURCE: Loiacono, Daniele, Luigi Cardamone, and Pier Luca Lanzi. "Simulated car racing championship: Competition software manual." arXiv preprint arXiv:1304.1672 (2013).
 */
class TORCSHandler(val id : Int, val driver : ANNRacer) {
    companion object
    {
        val MAX_STEPS = 0
        val UDP_TIMEOUT = 2000
        val VERBOSE = false
    }
    val mySocket = SocketHandler("localhost", 3000 + id, VERBOSE)
    var stage = ControllerStage.UNKNOWN
    val initStr : String

    init {
        driver.stage = stage
        driver.trackName = "unknown"


        /* Build init string */
        val angles = driver.initAngles()
        val initStr = StringBuilder("SCR(init")

        for (angle in angles) {
            initStr.append(" ").append(angle)
        }

        initStr.append(")")

        this.initStr = initStr.toString()

        sendID()
    }

    private fun sendID() {
        var inMsg: String?

        do {
            mySocket.send(initStr)
            inMsg = mySocket.receive(UDP_TIMEOUT)
        } while (inMsg == null || !inMsg.contains("***identified***"))
    }

    fun run() {
        var inMsg: String?
        var currentStep: Long = 0

        /*
         * Start to drive
         */while (true) {
            /*
             * Receives from TORCS the game state
             */
            inMsg = mySocket.receive(UDP_TIMEOUT)

            if (inMsg != null) {
                /*
                 * Check if race is ended (shutdown)
                 */
                if (inMsg.contains("***shutdown***")) {
                    break
                }

                /*
                 * Check if race is restarted
                 */if (inMsg.contains("***restart***")) {
                    break
                }

                var action = Action()
                if (currentStep < MAX_STEPS || MAX_STEPS == 0) {
                    action = driver.control(MessageBasedSensorModel(inMsg))
                } else {
                    action.restartRace = true
                }

                currentStep++
                mySocket.send(action.toString())
            }
        }

        /*
         * Shutdown the controller
         */
        driver.shutdown()
        mySocket.close()
    }
}