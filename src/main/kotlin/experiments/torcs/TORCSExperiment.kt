package experiments.torcs

import elements.phenotype.neuralnetworks.NetworkPhenotype
import experiments.Experiment
import experiments.Reference
import experiments.SampleVector
import experiments.torcs.annracer.ANNController
import experiments.torcs.annracer.ANNRacer
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import settings.SettingManager
import simtorcs.car.Car
import simtorcs.race.Race
import simtorcs.track.Track
import simtorcs.visualization.RaceAWTComponent
import util.io.PathUtil
import java.io.File
import javax.swing.JFrame
import kotlin.math.min

/**
 * The (sim)TORCS experiment.
 *
 * @property realTORCS If true, the real TORCS environment is applied. Otherwise, simTORCS.
 * @property loadTracks If true, the tracks defined in the settings are loaded from the respective XML-files.
 * @constructor Creates a new instance.
 *
 * @param id The ID of the experiment.
 * @param noise If true, the car sensors are noise affected.
 */
class TORCSExperiment(
    id: Int, val realTORCS: Boolean,  noise: Boolean, val loadTracks: Boolean = true
) : Experiment(
    id, 21, 3,  false, noise, !realTORCS
) {
    companion object {
        /**
         * Stores the simTORCS tracks by their respective names.
         */
        private val trackMap = hashMapOf<String, Track>()

        /**
         * Returns a track with the given level of detail.
         *
         * @param name The track name.
         * @param levelOfDetail The levle of detail, > 0.
         * @return The simTORCS track instance.
         */
        fun getTrack(name: String, levelOfDetail: Int): Track {
            if (trackMap.containsKey(name)) {
                return trackMap[name]!!
            }

            val track = Track.load(name, levelOfDetail)
            trackMap[name] = track

            return track
        }
    }


    override val name get() = if (realTORCS) "TORCS SCR" else "simTORCS"
    override val evaluateEachSolutionOnce = true

    val torcsInterface = TORCSInterface(PathUtil.workingDir + "external/torcs",PathUtil.workingDir + "external/torcs/customconfig",noise)

    /**
     * List of considered simTORCS tracks.
     */
    val tracks = mutableListOf<Track>()

    init {
        if (realTORCS && !File(PathUtil.workingDir + "external/TORCS").exists())
        {
            throw Exception("Please ensure that TORCS is installed in ${PathUtil.workingDir}external/TORCS.")
        }

        if (loadTracks) {
            var nextReferenceID = 0

            // Load the tracks.
            for (trackName in SettingManager.get("TORCS_SIMTORCS_TRACKS").value.split(",")) {
//          Use ![TrackName] to load a track with the turns being inverted:
                references.add(Reference(nextReferenceID++, trackName.trim()))
            }

            // Add the tracks as references.
            for (reference in references) {
                tracks.add(
                    getTrack(
                        reference.name,
                        SettingManager.get("TORCS_SIMTORCS_LEVEL_OF_DETAIL").getValueAsInt()
                    )
                )
            }
        }
    }


    override fun sampleAgainst(phenotype: NetworkPhenotype, reference: Reference): SampleVector {
        if (realTORCS) {
            // Run a TORCS process with ANNRacer.
            TODO("This part of the experiment is not part of my thesis. Implement the body of experiments.torcs.annracer.ANNRacer, method getFitness() first. Then remove this line.")

            return torcsInterface.createAndRunProcess(ANNRacer(phenotype), reference.id, false)
        } else {
            // Create and run a simTORCS race.
            val race = Race(tracks[reference.id], noise)
            val car_ = race.createCar().apply { setController(ANNController(phenotype.copy())) }

            race.run()

            // Determine the fitness of the network.
            val fitness = Array(getFitness(car_).size) { DescriptiveStatistics() }

            for (car in race.cars) {
                val carFitness = getFitness(car)

                for (index in carFitness.indices) {
                    fitness[index].addValue(carFitness[index])
                }
            }

            return SampleVector(phenotype.id, reference.id, Array(fitness.size) { i -> fitness[i].mean })
        }
    }

    override fun evaluate(networkPhenotype: NetworkPhenotype, referenceID : Int) {
        /**
         * Maximum time in seconds.
         */
        val tMaxInSeconds = 120

        /**
         * Speed up the visualization by a certain factor. simTORCS only.
         */
        val timeMultiplier = 8.0

        if (realTORCS) {
            println("IMPORTANT NOTE: For visual observation you have run TORCS manually: 1. Run wtorcs.exe, 2. Select Race -- Quick Race -- New Race (Please ensure that the only driver in the race is Driver 'scr_server 1'). 3. Run the evaluation within this program, it will catch up the running wtorcs process.")

            val result = torcsInterface.createAndRunProcess(ANNRacer(networkPhenotype), referenceID, false)


            println("Fitness of car: " + result.objectives.contentToString())
        } else {

            // Create a race on Brondehach without noise. End the race after _tMaxInSeconds_ [s]econds.
            val race = Race(tracks[referenceID], false, Race.FPS * tMaxInSeconds)
            val car = race.createCar().apply { setController(ANNController(networkPhenotype)) }

            val jFrame = JFrame("simTORCS Race on ${race.track.name}")
            jFrame.add(RaceAWTComponent(race, 850))
            jFrame.setSize(900, 900)
            jFrame.isVisible = true

            while (race.tNow < race.tMax && !race.raceFinished) {
                // Use the update-call to proceed step-wise. race.run() would process the whole race at once.
                race.update()

                // If all cars are disqualified, stop the race.
                if (race.cars.all { it.disqualified }) break

                jFrame.repaint()
                Thread.sleep((1000.0 / (timeMultiplier * Race.FPS)).toLong())
            }

            println("Fitness of car: " + getFitness(car).contentToString())
        }
    }

    /**
     * Example: The fitness functions described in my thesis.
     */
    fun getFitness(car: Car): Array<Double> {
        // Define the required constants:
        val metersPerTickAt180KMH = (180.0 / 3.6) * Race.DT
        val distanceAt180KMH = metersPerTickAt180KMH * car.race.tMax.toDouble()
        val distanceOnStraightsAt180KMH = distanceAt180KMH * (car.track.straightLength / car.track.length)

        val fDistance = 1.0 - min(1.0, car.sensorInformation.distanceRaced / distanceAt180KMH)
        val fTurnSpeed = if (car.sensorInformation.ticksInOrBeforeTurns == 0) 1.0 else
            (car.sensorInformation.tooLowTurnSpeed / (car.sensorInformation.ticksInOrBeforeTurns * Race.FPS).toDouble()).coerceIn(
                0.0,
                1.0
            )

        val fDrivenStraight =
            1.0 - min(1.0, car.sensorInformation.lengthDrivenStraight / distanceOnStraightsAt180KMH)

        return arrayOf(fDistance, fTurnSpeed, fDrivenStraight)
    }
}