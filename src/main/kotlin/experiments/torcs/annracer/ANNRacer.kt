package experiments.torcs.annracer

import elements.phenotype.neuralnetworks.NetworkPhenotype
import experiments.torcs.scr.Action
import experiments.torcs.scr.SensorModel
import experiments.torcs.scr.Controller
import kotlin.math.*

/**
 * ANNRacer for TORCS. A neural network controlled racing car bot.
 */
class ANNRacer(val network: NetworkPhenotype) : Controller() {
    companion object {
        /**
         * Max. inclination of the wheels in radians.
         */
        const val STEER_MAX_IN_RAD = 0.366519

        // Definition of the track edge sensors.
        const val NUM_OF_SENSORS = 19
        const val ANGLE_BETWEEN_SENSORS = 5.0
        const val LEFT_ANGLE = -ANGLE_BETWEEN_SENSORS * ((NUM_OF_SENSORS - 1) / 2)

        /**
         * Range of the track edge sensors in meters (<= 200).
         */
        const val SENSOR_RANGE = 150.0

        // Definition of speed and time constraints.
        const val SPEED_MIN = 50.0
        const val SPEED_MAX = 330.0
        const val MAX_STEPS = 10000

        const val MAX_DISTANCE_POSSIBLE = (MAX_STEPS.toDouble() * SPEED_MAX) / 180.0


        /**
         * Max. stuck frames before stuck routine takes control.
         */
        const val MAX_STUCK_TIME = 100
    }

    /**
     * True, if ANNRAcer is in training mode.
     */
    var isTraining = true


    override fun initAngles(): FloatArray {
        return FloatArray(NUM_OF_SENSORS) { i -> (LEFT_ANGLE + i * ANGLE_BETWEEN_SENSORS).toFloat() }
    }

    override fun shutdown() {
    }

    override fun reset() {
    }


    // Simulation and debugging data.
    var currentStep = 0
    var currentSpeed = 0.0
    var currentTrackPosition = 0.0
    var currentDamage = 0.0
    var speedReachedMax = 0.0

    // Fitness function relevance.
    var totalDistanceFromTrack = 0.0
    var currentDistanceRaced = 0.0

    // Stuck routine.
    var prevDistanceRaced = 0.0
    var stepsSinceLastMovement = 0
    var isFinallyStuck = false
    var isStuck = false
    var lastStepsOfStuckRoutine = false
    var canApplySpeedCriterion = false
    var timeStuck = 0

    override fun control(sensors: SensorModel?): Action {
        if (sensors != null) {
            // Update the sensors and create the neural network input vector.
            updateSensorInformation(sensors)

            val input = mutableListOf<Double>()
            // o_1: Speed.
            input.add(clamp(sensors.getSpeed() / SPEED_MAX))

            // o_2: Track position.
            input.add((min(1.0, max(-1.0, sensors.getTrackPosition())) + 1.0) / 2.0)

            // o_3 - o_21: Track edge sensors.
            for (sensor in sensors.getTrackEdgeSensors()) {
                input.add(clamp(sensor / SENSOR_RANGE))
            }

            // Activate the network.
            val output = network.update(input.toDoubleArray())

            val forceLeft = output[0]
            val forceRight = output[1]
            val targetSteer = forceLeft - forceRight
            val targetSpeed = SPEED_MIN + output[2] * (SPEED_MAX - SPEED_MIN)

            // Determine gas and brake. Taken over from SimpleDriver.
            val accelAndBrake = 2.0 / (1.0 + exp(sensors.getSpeed() - targetSpeed)) - 1.0

            val accel: Double
            val brake: Double

            if (accelAndBrake > 0) {
                accel = accelAndBrake
                brake = 0.0
            } else {
                accel = 0.0
                brake = filterABS(sensors, -accelAndBrake)
            }

            // Stuck- and Off-Track- routines.
            if (!isTraining) {
                if (isStuck) {
                    if (!lastStepsOfStuckRoutine) {
                        if (sensors.getTrackEdgeSensors()[9] > 15.0 || (sensors.getAngleToTrackAxis() * sensors.getTrackPosition() > 0.0)) {
                            lastStepsOfStuckRoutine = true
                        }
                    }

                    return stuckHandling(sensors, lastStepsOfStuckRoutine)
                } else {
                    updateStuck(sensors)
                }

                if (abs(sensors.getTrackPosition()) > 1.0) {
                    return offTrackHandling(sensors)
                }
            }

            // Create and commit the next action.
            val action = Action()
            action.gear = getGear(sensors)
            action.steering = targetSteer
            action.accelerate = accel
            action.brake = brake

            // If the car is stuck or the time is over, request finishing the race.
            if (isFinallyStuck || (isTraining && currentStep > MAX_STEPS)) action.restartRace = true

            return action
        }

        // Return an empty action, in case something got wrong.
        return Action()
    }

    fun clamp(input: Double, lower: Double = 0.0, upper: Double = 1.0): Double {
        return if (input < lower) lower else if (input > upper) upper else input
    }

    /**
     * Updates the relevant sensor information.
     *
     * @param sensors Parsed sensor values.
     */
    private fun updateSensorInformation(sensors: SensorModel) {
        currentSpeed = sensors.getSpeed()
        if (currentSpeed > 10.0) canApplySpeedCriterion = true

        currentTrackPosition = sensors.getTrackPosition()
        currentDamage = sensors.getDamage()

        prevDistanceRaced = currentDistanceRaced
        currentDistanceRaced = sensors.getDistanceRaced()

        if (currentSpeed > speedReachedMax) {
            speedReachedMax = currentSpeed
        }

        val absTrackPos = abs(currentTrackPosition)

        if (absTrackPos > 1.0) {
            totalDistanceFromTrack += absTrackPos
        }

        if (isTraining) {
            if (currentDistanceRaced - prevDistanceRaced > 1.0 / 50.0) {
                stepsSinceLastMovement = 0
            } else if (stepsSinceLastMovement++ >= MAX_STUCK_TIME) {
                isFinallyStuck = true
            }
        }

        currentStep++
    }

    /**
     * Returns the fitness of ANNRacer. The body of this function remains empty and has to be specified by the user.
     */
    fun getFitness(): Array<Double> {

        println("Note: The fitness is undefined for this type of experiment. Please refer to experiments.torcs.annracer.ANNRacer, method getFitness().")

        return Array(0) {0.0}

//        if (currentStep == 0) currentStep = 1
//
//        val fDistanceMoved = max(0.0, 1.0 - currentDistanceRaced / MAX_DISTANCE_POSSIBLE)
//        val fDamage = min(1.0, currentDamage / currentStep.toDouble())
//        val fOffRoad = min(1.0, totalDistanceFromTrack / (1.5 * currentStep.toDouble()))
//        val fSpeedMax = 1.0 - min(1.0, speedReachedMax / SPEED_MAX)
//
//        val nonNorm = arrayOf(fDistanceMoved, fDamage, fOffRoad, fSpeedMax)
//
//        return Array(nonNorm.size) { i -> clamp(nonNorm[i]) }
    }


    /**
     * ASSISTANT SYSTEMS.
     **/


    // Gear Changing Constants of Autopia.
    val gearUp = intArrayOf(9500, 9500, 9500, 9500, 9000, 0)
    val gearDown = intArrayOf(0, 4000, 6300, 7000, 7300, 7300)

    /**
     * Gear changing of SimpleDriver.
     */
    private fun getGear(sensors: SensorModel): Int {
        val gear = sensors.getGear()
        val rpm = sensors.getRPM()

        if (gear < 1) return 1

        return when {
            gear < 6 && rpm >= gearUp[gear - 1] -> gear + 1
            gear > 1 && rpm <= gearDown[gear - 1] -> gear - 1
            else -> gear
        }
    }


    // ABS Filter Constants
    val wheelRadius =
        doubleArrayOf(0.3306, 0.3306, 0.3276, 0.3276)
    val absSlip = 2.0
    val absRange = 3.0
    val absMinSpeed = 3.0

    /**
     * ABS of SimpleDriver.
     */
    private fun filterABS(sensors: SensorModel, brakeIn: Double): Double { // convert speed to m/s
        var brakeOut = brakeIn
        val speed = (sensors.getSpeed() / 3.6)
        // when spedd lower than min speed for abs do nothing
        if (speed < absMinSpeed) return brakeOut

        // compute the speed of wheels in m/s
        val wheelSpinVelocity = sensors.getWheelSpinVelocity()
        var slip = 0.0

        for (i in 0..3) {
            slip += wheelSpinVelocity[i] * wheelRadius[i]
        }
        // slip is the difference between actual speed of car and average speed of wheels
        slip = speed - slip / 4.0
        // when slip too high applu ABS
        if (slip > absSlip) {
            brakeOut = (brakeOut - (slip - absSlip) / absRange)
        }
        // check brake is not negative, otherwise set it to zero
        return if (brakeOut < 0.0) 0.0 else brakeOut
    }


    /**
     * Off-track handling of Autopia.
     *
     * @param sensors The parsed sensor values.
     *
     * @return The next action to commit.
     */
    fun offTrackHandling(sensors: SensorModel): Action {
        val action = Action()
        action.steering = clamp((sensors.getAngleToTrackAxis() - sensors.getTrackPosition() * 0.5) / STEER_MAX_IN_RAD, -1.0, 1.0)
        action.accelerate = 0.3
        action.gear = getGear(sensors)

        return action
    }

    /**
     * Stuck handling of Autopia.
     *
     * @param sensors The parsed sensor values.
     * @param clear If true, leave the routine.
     * @return The next action to commit.
     */
    fun stuckHandling(sensors: SensorModel, clear: Boolean): Action {
        val action = Action()

        if (clear) {
            // Stuck-handling is finished. Prepare everything for the neural network to take control again.
            if (sensors.getSpeed() < 0.0 && abs(sensors.getSpeed()) > 0.1) {
                // Brake the car before handing control to the network.
                action.brake = 0.5
            } else {
                // Give control back to the network.
                lastStepsOfStuckRoutine = false
                isStuck = false
                timeStuck = 0
                canApplySpeedCriterion = false

                action.gear = 1
            }

            return action
        }


        // Main part of stuck-handling.
        action.gear = -1

        if (sensors.getSpeed() > 0.1) {
            // Step 1.
            action.brake = 0.5
        } else {
            action.accelerate = 1.0
            action.steering = -sensors.getAngleToTrackAxis() / STEER_MAX_IN_RAD
        }

        return action
    }

    /**
     * Checks and updates the stuck conditions.
     *
     * @param sensors The parsed sensor values.
     */
    fun updateStuck(sensors: SensorModel) {
        val conditionAngle = abs(sensors.getAngleToTrackAxis()) > PI / 6.0
        val conditionDeviation = abs(sensors.getTrackPosition()) > 0.5

        val conditionSpeed = canApplySpeedCriterion && sensors.getSpeed() < 10.0

        val isStuckThisTick = (conditionAngle && conditionDeviation) || conditionSpeed


        if (isStuckThisTick) {
            isStuck = timeStuck++ > 50
        } else {
            timeStuck = 0
        }
    }
}