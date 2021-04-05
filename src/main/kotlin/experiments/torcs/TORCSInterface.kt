package experiments.torcs

import experiments.SampleVector
import experiments.torcs.annracer.ANNRacer
import experiments.torcs.scr.TORCSHandler
import kotlinx.coroutines.*
import util.io.PathUtil
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Interface to 'real' TORCS.
 *
 * torcsDir: Path to the TORCS installation (with SCR patch).
 * configDir: Path to the custom race configurations. Note that the configurations have to be named: c[configID].xml. For example: c0.xml is applied, if createAndRunProcess is called with configID = 0.
 */
class TORCSInterface(val torcsDir: String = PathUtil.workingDir + "torcs", val configDir : String = PathUtil.workingDir + "torcs/customconfig", val noise: Boolean) {

    companion object
    {
        // Timeout in real time [ms].
        val RACE_TIMEOUT = 500_000L
    }

    val command = "\"$torcsDir/wtorcs.exe\" ${if (noise) "-noisy " else ""}-nofuel -nodamage -nolaptime -r"

    /**
     * Creates a TORCS process and runs a race with the provided instance of ANNRacer.
     *
     * @param racer The instance of ANNRacer.
     * @param configID The ID of the TORCS configuration to load (determines the track number).
     * @param printErrors If true, errors are printed to the console.
     * @return The sample vector containing fitness information about ANNRacer.
     */
    fun createAndRunProcess(
        racer: ANNRacer,
        configID: Int,
        printErrors: Boolean = false
    ): SampleVector {

        var p = runProcess(printErrors, configID)

        val job = GlobalScope.launch {
            TORCSHandler(1, racer).run()
        }

        var runFinished = runJob(job, p)

        while (!runFinished) {
            if (p.isAlive) p.destroy()
            p = runProcess(printErrors, configID)
            println("Restarting evaluation due to timeout.")

            runFinished = runJob(job, p)
        }

        return SampleVector(racer.network.id, configID, racer.getFitness())
    }

    /**
     * Runs the TORCS process with the given configuration.
     */
    private fun runProcess(printErrors: Boolean = false, configID: Int): Process {
        val cmd_ = command + " \"" + configDir + "/c${configID}.xml" + "\""
        val p = Runtime.getRuntime().exec(cmd_, null, File(torcsDir))

        if (printErrors) {
            val inputStream: InputStream = p.inputStream
            val inputStreamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            var line: String?

            while (bufferedReader.readLine().also { line = it } != null) {
                println(line)
            }
        }

        return p
    }

    /**
     * Run the TORCS process. Aborts after RACE_TIMEOUT ms.
     */
    private fun runJob(job: Job, p: Process): Boolean {
        return try {
            runBlocking {
                withTimeout(RACE_TIMEOUT)
                {
                    job.join()
                }
            }

            if (p.isAlive) p.destroy()

            true
        } catch (ex: Exception) {
            false
        }
    }
}