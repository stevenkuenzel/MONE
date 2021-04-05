package experiments.torcs.annracer

import elements.phenotype.neuralnetworks.NetworkPhenotype
import simtorcs.car.SensorInformation
import simtorcs.car.control.CarController
import simtorcs.car.control.CarInput
import simtorcs.race.Race
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * ANNRacer for simTORCS. A neural network controlled racing car bot.
 */
class ANNController(val network: NetworkPhenotype) : CarController() {

    override fun control(si: SensorInformation): CarInput {
        // Create the input.
        val input = DoubleArray(2 + si.trackEdgeSensors.size)
        var inputIndex = 0
        input[inputIndex++] = min(1.0, si.absoluteVelocity / Race.SPEED_MAX)
        input[inputIndex++] = (min(1.0, max(-1.0, si.distanceToTrackAxis)) + 1.0) / 2.0

        for (d in si.trackEdgeSensors) {
            input[inputIndex++] = d
        }

        // Activate the network.
        val output = network.update(input)

        // Set the car controls.
        val forceLeft = output[0]
        val forceRight = output[1]

        val targetSpeed =
            Race.SPEED_MIN + output[2] * (Race.SPEED_MAX - Race.SPEED_MIN)
        val accelAndBrake = 2.0 / (1.0 + exp(si.absoluteVelocity - targetSpeed)) - 1.0

        return CarInput(
            forceLeft,
            forceRight,
            if (accelAndBrake > 0.0) accelAndBrake else 0.0,
            if (accelAndBrake < 0.0) -accelAndBrake else 0.0
        )
    }
}