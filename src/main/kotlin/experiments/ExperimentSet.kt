package experiments

import algorithms.AbstractAlgorithm
import de.stevenkuenzel.xml.XAttribute
import de.stevenkuenzel.xml.XElement
import de.stevenkuenzel.xml.XSavable
import settings.*
import util.io.PathUtil
import java.util.*

/**
 * A set of experiment instances, i.e., multiple repetitions of the same experiment.
 *
 * @constructor Creates a new instance.
 */
class ExperimentSet private constructor() : XSavable {
    companion object {
        val excluded =
            arrayOf("ALGORITHM", "PARAMETER_CONTROLLER", "NNEAT_Q_PROCEDURE", "EXPERIMENT", "POPULATION_SIZE")

        /**
         * Creates a new instance of ExperimentSet for the according configuration.
         *
         * @param populationSize The population size.
         * @param eAlgorithm The multi-objective neuroevolutionary algorithm (MONA).
         * @param eParameterController The parameter controller.
         * @param eExperiment The experiment.
         * @param eQProcedure The q-Procedure for the MONA. Only nNEAT.
         * @return An instance of ExperimentSet.
         */
        fun create(
            populationSize: Int,
            eAlgorithm: EnumAlgorithm,
            eParameterController: EnumParameterController,
            eExperiment: EnumExperiment,
            eQProcedure: EnumQProcedure? = null
        ): ExperimentSet {
            val result = ExperimentSet()

            // Copy the non-excluded settings.
            for (setting in SettingManager.global.settings) {
                if (excluded.contains(setting.name)) continue

                result.settings.add(Pair(setting.name, setting.value))
            }

            // Set the remaining ones, relative to the parameters of this method.
            result.settings.add(Pair("POPULATION_SIZE", populationSize.toString()))
            result.settings.add(Pair("ALGORITHM", eAlgorithm.id.toString()))
            result.settings.add(Pair("PARAMETER_CONTROLLER", eParameterController.id.toString()))
            result.settings.add(Pair("EXPERIMENT", eExperiment.id.toString()))

            if (eExperiment == EnumExperiment.FightingICE)
            {
                // This information is required to distinguish between ANNBot1 and ANNBot2 (the former was relevant only in the context of my thesis), so ANNBot2 is default.
                result.settings.add(Pair("FURTHER", "ANNBot2"))
            }

            if (eQProcedure != null) result.settings.add(Pair("NNEAT_Q_PROCEDURE", eQProcedure.id.toString()))

            return result
        }
    }

    /**
     * Unique id of this experiment set.
     */
    private val uuid = UUID.randomUUID().toString()

    /**
     * Contains all instances of MONAs running within this experiment set.
     */
    private val iterations = mutableListOf<AbstractAlgorithm<*>>()

    /**
     * The user's settings.
     */
    val settings = mutableListOf<Pair<String, String>>()

    /**
     * Adds a new MONA instance to this experiment set.
     *
     * @param iteration The MONA instance.
     */
    fun registerIteration(iteration: AbstractAlgorithm<*>) {
        iterations.add(iteration)
        iteration.onTermination += { checkTerminated() }
    }


    /**
     * Checks whether all MONA instances of this experiment set have terminated. If so, calls the _save_ method.
     *
     */
    fun checkTerminated() {
        if (iterations.all { it.terminated }) {
            println("The full experiment set ($uuid) has been finished.")
            save()
        }
    }

    /**
     * Saves the results of this experiment set to an XML-file.
     *
     */
    fun save() {
        toXElement().save(PathUtil.outputDir + "Experiment_${uuid}.xml")
    }

    override fun toXElement(): XElement {
        val xElement = XElement("Experiment")
        val xProperties = xElement.addChild("Properties")

        for (pair in settings.sortedBy { it.first }) {
            xProperties.addChild("Property", XAttribute("Name", pair.first), XAttribute("Value", pair.second))
        }

        val xIterations = xElement.addChild("Iterations", XAttribute("Size", iterations.size))

        for (iteration in iterations) {
            xIterations.addChild("Iteration", XAttribute("File", iteration.uuid + ".xml"))
        }

        return xElement
    }
}