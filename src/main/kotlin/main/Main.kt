package main

import algorithms.AbstractAlgorithm
import controlparameters.Parameter
import de.stevenkuenzel.xml.XElement
import elements.phenotype.neuralnetworks.NetworkPhenotype
import experiments.ExperimentSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import postprocessing.experiments.ExpResultLoader
import settings.*
import sorting.impl.CrowdingDistance
import util.io.PathUtil
import util.random.RandomProvider
import java.io.File

/**
 * The entry point of the project. See the function _main_ in the companion object.
 *
 */
class Main {
    companion object {
        init {
            // Load or create the user-settings.
            SettingManager.loadOrCreate(File(PathUtil.inputDir + "config.json"))
        }

        @JvmStatic
        fun main(args: Array<String>) {
            // Initialize the RNG provider.
            RandomProvider.initialize(SettingManager.get("RANDOM_SEED").getValueAsLong())

            // Select the mode to run.
            if (args.isNotEmpty() && args[0] == "-eval") {
                eval()
            }
            else if (args.isNotEmpty() && args[0] == "-postprocess") {
                postprocess()
            } else {
                run()
            }
        }

        /**
         * Evaluates a neural network. The file path is 'input/ANN.xml'.
         *
         * referenceID determines the entry from the references in 'input/config.json' that is used for evaluation (only for FightingICE and TORCS).
         */
        private fun eval(referenceID: Int = 0) {
            val networkPath = PathUtil.inputDir + "ANN.xml"

            if (!File(networkPath).exists()) {
                throw Exception("IMPORTANT NOTE: Could not find a neural network. Place the exported neural network as an XML file named 'ANN.xml' into the 'input' directory.")
            }

            val xElement = XElement.load(networkPath)!!
            val network = NetworkPhenotype.fromXElement(xElement)

            val experimentID = SettingManager.get("EXPERIMENT").value.split(",").first().trim().toInt()
            val eExperiment = EnumExperiment.values().find { x -> x.id == experimentID }!!
            val experiment = eExperiment.getCopyOfExperiment(-1)

            experiment.evaluate(network, referenceID)
        }

        /**
         * Evaluates the experimental results stored in the 'input' dir. The evaluations are described in the file 'input/analyse.xml'.
         *
         */
        private fun postprocess() {
            ExpResultLoader.load(PathUtil.inputDir + "analyse.xml")
        }

        /**
         * Runs a(n) (set of) experiment(s) according to the user-settings.
         */
        private fun run() {
            // Create the experiments to run, based on the configuration at hand.
            val listOfAlgorithms = createListOfAlgorithms()

            val runConcurrently = SettingManager.get("EXPERIMENTS_RUN_ALL_CONCURRENTLY").getValueAsBoolean()

            if (!runConcurrently) {
                for (algorithm in listOfAlgorithms) {
                    runWithInfo(algorithm)
                }
            } else {
                val scope = CoroutineScope(Dispatchers.Default) // Alternative: newFixedThreadPoolContext

                runBlocking(scope.coroutineContext) {
                    for (algorithm in listOfAlgorithms) {
                        launch {
                            runWithInfo(algorithm)
                        }
                    }
                }
            }
        }


        /**
         * Creates an instance of an algorithm.
         *
         * @param populationSize Population size.
         * @param experimentID Experiment ID.
         * @param eExperiment Selected experiment.
         * @param eAlgorithm Selected algorithm.
         * @param eParameterController Selected parameter controller.
         * @param eQProcedure Selected q-Procedure (only nNEAT or EMOA).
         * @return The algorithm instance.
         */
        private fun createAlgorithm(
            populationSize: Int,
            experimentID: Int,
            eExperiment: EnumExperiment,
            eAlgorithm: EnumAlgorithm,
            eParameterController: EnumParameterController,
            eQProcedure: EnumQProcedure?
        ): AbstractAlgorithm<*> {
            val qProcedure = eQProcedure?.getCopyOfQProcedure()
            val exp = eExperiment.getCopyOfExperiment(experimentID)

            val alg = eAlgorithm.getCopyOfAlgorithm(exp, qProcedure)
            alg.initialize()
            alg.set(Parameter.Population_Size, populationSize)

            val pc = eParameterController.getCopyOfParameterController(alg, alg.random)
            alg.registerParameterController(pc, CrowdingDistance())
            alg.onEpochFinished += {

                println(
                    it.experiment.id.toString() + ": " + it.evaluations + ": " + it.getMeanFitness().contentToString()
                )

                if (SettingManager.get("EXPORT_PARETO_FRONT_EVERY_GENERATION").getValueAsBoolean()) {
                    it.exportPhenotypes()
                }
            }

            alg.onTermination += {
                if (SettingManager.get("EXPORT_PARETO_FRONT_AFTER_TERMINATION").getValueAsBoolean()) {
                    it.exportPhenotypes()
                }
            }

            return alg
        }

        /**
         * Runs the evolutionary process of an algorithm. Prints log-messages.
         *
         * @param algorithm The algorithm instance.
         */
        private fun runWithInfo(algorithm: AbstractAlgorithm<*>) {
            println("Starting experiment ${algorithm.experiment}.")
            algorithm.run()
            println("Ending experiment ${algorithm.experiment}.")
        }

        /**
         * Creates all algorithm instances to run. According to the user-settings.
         *
         * @return List of algorithms to run.
         */
        private fun createListOfAlgorithms(): List<AbstractAlgorithm<*>> {
            val experimentRepetitions = SettingManager.get("EXPERIMENT_REPETITIONS").getValueAsInt()

            // Population sizes.
            val strsizes = SettingManager.get("POPULATION_SIZE").value.split(",")
            val populationSizes = strsizes.map { it.trim().toInt() }

            // Experiments.
            val experiments = mutableListOf<EnumExperiment>()

            SettingManager.get("EXPERIMENT").value.split(",").forEach {
                val id = it.trim().toInt()
                val experiment = EnumExperiment.values().find { x -> x.id == id }

                if (experiment != null) {
                    experiments.add(experiment)
                } else {
                    throw Exception("Experiment with id $id not found.")
                }
            }

            // Algorithms.
            val algorithms = mutableListOf<EnumAlgorithm>()

            SettingManager.get("ALGORITHM").value.split(",").forEach {
                val id = it.trim().toInt()
                val algorithm = EnumAlgorithm.values().find { x -> x.id == id }

                if (algorithm != null) {
                    algorithms.add(algorithm)
                } else {
                    throw Exception("Algorithm with id $id not found.")
                }
            }

            // Parameter Controllers.
            val controllers = mutableListOf<EnumParameterController>()

            SettingManager.get("PARAMETER_CONTROLLER").value.split(",").forEach {
                val id = it.trim().toInt()
                val controller = EnumParameterController.values().find { x -> x.id == id }

                if (controller != null) {
                    controllers.add(controller)
                } else {
                    throw Exception("Parameter controller with id $id not found.")
                }
            }

            // qProcedures.
            val qProcedures = mutableListOf<EnumQProcedure>()

            SettingManager.get("NNEAT_Q_PROCEDURE").value.split(",").forEach {
                val id = it.trim().toInt()
                val qProcedure = EnumQProcedure.values().find { x -> x.id == id }

                if (qProcedure != null) {
                    qProcedures.add(qProcedure)
                } else {
                    throw Exception("qProcedure with id $id not found.")
                }
            }


            var nextExperimentID = 0
            val listOfAlgorithms = mutableListOf<AbstractAlgorithm<*>>()

            for (eExperiment in experiments) {
                for (eAlgorithm in algorithms) {
                    for (eParameterController in controllers) {

                        if (eAlgorithm != EnumAlgorithm.NEAT_MODS && eAlgorithm != EnumAlgorithm.NEAT_PS) {
                            for (eQProcedure in qProcedures) {
                                for (populationSize in populationSizes) {
                                    val expSet =
                                        ExperimentSet.create(
                                            populationSize,
                                            eAlgorithm,
                                            eParameterController,
                                            eExperiment,
                                            eQProcedure
                                        )

                                    for (i in 0 until experimentRepetitions) {
                                        listOfAlgorithms.add(
                                            createAlgorithm(
                                                populationSize,
                                                nextExperimentID++,
                                                eExperiment,
                                                eAlgorithm,
                                                eParameterController,
                                                eQProcedure
                                            ).also { expSet.registerIteration(it) }
                                        )
                                    }
                                }
                            }
                        } else {

                            for (populationSize in populationSizes) {
                                val expSet =
                                    ExperimentSet.create(populationSize, eAlgorithm, eParameterController, eExperiment)

                                for (i in 0 until experimentRepetitions) {
                                    listOfAlgorithms.add(
                                        createAlgorithm(
                                            populationSize,
                                            nextExperimentID++,
                                            eExperiment,
                                            eAlgorithm,
                                            eParameterController,
                                            null // Default qProcedure.
                                        ).also { expSet.registerIteration(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            return listOfAlgorithms
        }
    }
}
