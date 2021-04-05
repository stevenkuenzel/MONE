package experiments.dpb

import elements.phenotype.neuralnetworks.NetworkPhenotype
import experiments.Experiment
import experiments.Reference
import experiments.SampleVector
import org.apache.commons.math3.util.FastMath
import javax.swing.JFrame
import kotlin.math.abs

/**
 * The multi-objective Double Pole Balancing expierment.
 *
 * @property withVelocities True, if the cart/pole velocities are provided to the network.
 * @constructor Creates a new instance.
 *
 * @param id ID of the experiment.
 * @param noise True, if the pole angles are inclined randomly.
 */
class DoublePoleBalancing(id: Int, val withVelocities: Boolean, noise: Boolean) :
    Experiment(id, if (withVelocities) 6 else 3, 1, false, noise, true) {

    override val name = "moDPB"
    override val evaluateEachSolutionOnce = true

    init {
        // Add the default scenario as (only) reference.
        references.add(Reference(-1, "Default"))
    }

    override fun sampleAgainst(phenotype: NetworkPhenotype, reference: Reference): SampleVector {
        return SampleVector(phenotype.id, reference.id, DPBInstance(phenotype, noise, this).run())
    }

    override fun evaluate(networkPhenotype: NetworkPhenotype, referenceID: Int) {

        // _referenceID_ is not used here, as there is only a single reference (default).

        val dpb = DPBInstance(networkPhenotype, noise, this)
        val dpbVisualization = DPBVisualization(
            2.0 * DPBInstance.TRACK_LENGTH_HALFED,
            arrayOf(DPBInstance.poleLength1, DPBInstance.poleLength2)
        )

        val jFrame = JFrame("Multi-Objective Double Pole Balancing (Noise: $noise)")
        jFrame.add(dpbVisualization)
        jFrame.setSize(900, 900)
        jFrame.isVisible = true

        while (dpb.step()) {
            dpbVisualization.step(dpb.state[0], arrayOf(dpb.state[2], dpb.state[4]))
            jFrame.repaint()
            Thread.sleep((1000.0 * DPBInstance.DT).toLong())
        }

        println("Fitness: ${dpb.getFitness().contentToString()}")
    }


    /**
     * The experiment instance. Based on the implementation provided in ANJI.
     *
     * SOURCE: http://anji.sourceforge.net/
     *
     * @property network The network controller.
     * @property randomAngles True, if the poles are inclined randomly.
     * @property experiment Reference to the experiment instance.
     * @constructor Creates a new instance.
     */
    class DPBInstance(
        val network: NetworkPhenotype,
        private val randomAngles: Boolean,
        val experiment: DoublePoleBalancing
    ) {
        companion object {
            // Physical and model constants.

            val GRAVITY = -9.80665
            val MASSCART = 1.0
            val FORCE_MAG = 10.0
            val MUP = 0.000002
            val DT = 0.01
            val poleLength1 = 0.5
            val poleMass1 = 0.1
            val poleLength2 = 0.05
            val poleMass2 = 0.01
            val POLE_1_INITIAL_ANGLE = Math.PI / 180.0
            val POLE_2_INITIAL_ANGLE = 0.0
            val MAX_POLE_INITIAL_ANGLE = Math.PI / 20.0
            val POLE_ANGLE_THRESHOLD = Math.PI / 5.0
            val TRACK_LENGTH_HALFED = 2.4
            var DEFAULT_TIMESTEPS = 10000
        }

        /**
         * Current state of the cart and the poles.
         */
        val state: Array<Double> = createInitialState()

        /**
         * Creates the initial state. Pole angles are either default or random within the defined borders.
         *
         * @return The initial state array.
         */
        private fun createInitialState(): Array<Double> {
            val state = Array(6) { 0.0 }

            if (randomAngles) {
                state[2] = (experiment.random.nextDouble() - 0.5) * 2.0 * MAX_POLE_INITIAL_ANGLE
                state[4] = (experiment.random.nextDouble() - 0.5) * 2.0 * MAX_POLE_INITIAL_ANGLE
            } else {
                state[2] = POLE_1_INITIAL_ANGLE
                state[4] = POLE_2_INITIAL_ANGLE
            }

            return state
        }


        val input = Array(experiment.numOfInputs) { 0.0 }

        /**
         * Time t.
         */
        var currentTimestep = 0

        /**
         * Summed distance of the cart to the track center.
         */
        var distanceToCenter = 0.0


        /**
         * Last direction the force affected the cart. Used for determining the number of direction changes.
         */
        var lastDirection = 0

        /**
         * Counts the number of force direction changes.
         */
        var dirChanges = 0


        /**
         * Runs the complete experiment and returns a fitness vector.
         *
         * @return The fitness vector.
         */
        fun run(): Array<Double> {
            while (currentTimestep < DEFAULT_TIMESTEPS) {
                if (!step()) break
            }

            return getFitness()
        }

        /**
         * Perform a single step. Returns false if the experiment is failed.
         *
         * @return Flag that determines whether the experiment has failed in the current timestep.
         */
        fun step(): Boolean {
            if (experiment.withVelocities) {
                // Markovian (With velocity info)
                input[0] = state[0] / TRACK_LENGTH_HALFED // Cart position.
                input[1] = state[1] / 0.75 // Cart velocity.
                input[2] = state[2] / POLE_ANGLE_THRESHOLD // Pole 1 angle.
                input[3] = state[3] // Pole 1 angular velocity.
                input[4] = state[4] / POLE_ANGLE_THRESHOLD // Pole 2 angle.
                input[5] = state[5] // Pole 2 angular velocity.

            } else {
                // Non-markovian (without velocity info)
                input[0] = state[0] / TRACK_LENGTH_HALFED // Cart position.
                input[1] = state[2] / POLE_ANGLE_THRESHOLD // Pole 1 angle.
                input[2] = state[4] / POLE_ANGLE_THRESHOLD // Pole 2 angle.
            }

            // Activate the network.
            val force = network.update(input)[0]
            val forceDirection = force - 0.5

            if (forceDirection > 0) {
                if (lastDirection == -1) {
                    dirChanges++
                }

                lastDirection = 1
            } else if (forceDirection < 0) {
                if (lastDirection == 1) {
                    dirChanges++
                }

                lastDirection = -1
            }

            // Let the force affect the cart.
            performAction(force, state)

            // Update the distance to the track center.
            distanceToCenter += abs(state[0])

            // Check for failure state. Has the cart run off the ends of the track or has the pole angle gone beyond the threshold.
            //
            if (state[0] < -TRACK_LENGTH_HALFED || state[0] > TRACK_LENGTH_HALFED ||
                state[2] > POLE_ANGLE_THRESHOLD || state[2] < -POLE_ANGLE_THRESHOLD ||
                state[4] > POLE_ANGLE_THRESHOLD || state[4] < -POLE_ANGLE_THRESHOLD
            ) {
                return false
            }

            currentTimestep++

            return true
        }

        /**
         * Returns the fitness vector determined throughout the experiment.
         *
         * @return The fitness vector.
         */
        fun getFitness(): Array<Double> {
            return arrayOf(
                (DEFAULT_TIMESTEPS - currentTimestep).toDouble() / DEFAULT_TIMESTEPS.toDouble(),
                dirChanges.toDouble() / currentTimestep.toDouble(),
                distanceToCenter / (currentTimestep.toDouble() * TRACK_LENGTH_HALFED)
            )
        }


        /**
         * PHYSICS RELATED VARIABLES AND METHODS.
         */

        private val dydx = Array(6) { 0.0 }
        private val RK4 = true //Set to Runge-Kutta 4th order integration method
        private val EULER_TAU = DT / 4.0

        private var force = 0.0
        private var costheta_1 = 0.0
        private var costheta_2 = 0.0
        private var sintheta_1 = 0.0
        private var sintheta_2 = 0.0
        private var gsintheta_1 = 0.0
        private var gsintheta_2 = 0.0
        private var temp_1 = 0.0
        private var temp_2 = 0.0
        private var ml_1 = 0.0
        private var ml_2 = 0.0
        private var fi_1 = 0.0
        private var fi_2 = 0.0
        private var mi_1 = 0.0
        private var mi_2 = 0.0

        private var hh = 0.0
        private var h6 = 0.0
        private val dym = Array(6) { 0.0 }
        private val dyt = Array(6) { 0.0 }
        private val yt = Array(6) { 0.0 }


        private fun performAction(output: Double, state: Array<Double>) {
            var i: Int

            /*--- Apply action to the simulated cart-pole ---*/
            if (RK4) {
                i = 0
                while (i < 2) {
                    dydx[0] = state[1]
                    dydx[2] = state[3]
                    dydx[4] = state[5]
                    step(output, state, dydx)
                    rk4(output, state, dydx, state)
                    ++i
                }
            } else {
                i = 0
                while (i < 8) {
                    step(output, state, dydx)
                    state[0] += EULER_TAU * dydx[0]
                    state[1] += EULER_TAU * dydx[1]
                    state[2] += EULER_TAU * dydx[2]
                    state[3] += EULER_TAU * dydx[3]
                    state[4] += EULER_TAU * dydx[4]
                    state[5] += EULER_TAU * dydx[5]
                    ++i
                }
            }
        }

        private fun step(action: Double, st: Array<Double>, derivs: Array<Double>) {
            force = (action - 0.5) * FORCE_MAG * 2

            costheta_1 = FastMath.cos(st[2])
            sintheta_1 = FastMath.sin(st[2])
            gsintheta_1 = GRAVITY * sintheta_1
            costheta_2 = FastMath.cos(st[4])
            sintheta_2 = FastMath.sin(st[4])
            gsintheta_2 = GRAVITY * sintheta_2

            ml_1 = poleLength1 * poleMass1
            ml_2 = poleLength2 * poleMass2
            temp_1 = MUP * st[3] / ml_1
            temp_2 = MUP * st[5] / ml_2

            fi_1 = ml_1 * st[3] * st[3] * sintheta_1 + 0.75 * poleMass1 * costheta_1 * (temp_1 + gsintheta_1)

            fi_2 = ml_2 * st[5] * st[5] * sintheta_2 + 0.75 * poleMass2 * costheta_2 * (temp_2 + gsintheta_2)

            mi_1 = poleMass1 * (1 - 0.75 * costheta_1 * costheta_1)
            mi_2 = poleMass2 * (1 - 0.75 * costheta_2 * costheta_2)

            derivs[1] = (force + fi_1 + fi_2) / (mi_1 + mi_2 + MASSCART)
            derivs[3] = -0.75 * (derivs[1] * costheta_1 + gsintheta_1 + temp_1) / poleLength1
            derivs[5] = -0.75 * (derivs[1] * costheta_2 + gsintheta_2 + temp_2) / poleLength2
        }

        private fun rk4(f: Double, y: Array<Double>, dydx: Array<Double>, yout: Array<Double>) {
            var i = 0

            hh = DT * 0.5
            h6 = DT / 6.0

            while (i <= 5) {
                yt[i] = y[i] + hh * dydx[i]
                i++
            }

            step(f, yt, dyt)

            dyt[0] = yt[1]
            dyt[2] = yt[3]
            dyt[4] = yt[5]

            i = 0
            while (i <= 5) {
                yt[i] = y[i] + hh * dyt[i]
                i++
            }

            step(f, yt, dym)

            dym[0] = yt[1]
            dym[2] = yt[3]
            dym[4] = yt[5]

            i = 0
            while (i <= 5) {
                yt[i] = y[i] + DT * dym[i]
                dym[i] += dyt[i]
                i++
            }

            step(f, yt, dyt)

            dyt[0] = yt[1]
            dyt[2] = yt[3]
            dyt[4] = yt[5]

            i = 0
            while (i <= 5) {
                yout[i] = y[i] + h6 * (dydx[i] + dyt[i] + 2.0 * dym[i])
                i++
            }
        }
    }
}