package elements.phenotype.neuralnetworks

import elements.genotype.neuralnetworks.NetworkGenotype
import elements.innovation.NeuronType
import elements.phenotype.neuralnetworks.activation.ElliotSigmoid
import de.stevenkuenzel.xml.XAttribute
import de.stevenkuenzel.xml.XElement
import de.stevenkuenzel.xml.XLoadable
import elements.genotype.Genotype
import elements.innovation.InnovationManager
import elements.phenotype.Phenotype
import util.random.RandomProvider

/**
 * A neural network phenotype
 *
 * @property neurons The list of neurons the network consists of (including the according incoming and outgoing links).
 *
 * @constructor Creates a new instance of NetworkPhenotype.
 *
 * @param id The ID of the phenotype.
 * @param genotype The Genotype instance that created this Phenotype instance.
 */
class NetworkPhenotype(id: Int, val neurons: List<Neuron>, genotype: Genotype<out Any>) : Phenotype(id, genotype) {

    companion object : XLoadable<NetworkPhenotype> {

        /**
         * Only for loading a neural network from the disk.
         */
        override fun fromXElement(xElement: XElement, vararg optional: Any): NetworkPhenotype {
            val id = xElement.getAttributeValueAsInt("ID")

            val neurons = mutableListOf<Neuron>()
            val neuronMap = hashMapOf<Int, Neuron>()

            // Load the neurons.
            val xNeurons = xElement.getChild("Neurons")!!
            xNeurons.getChildren("Neuron").forEach {
                val neuron = Neuron(
                    it.getAttributeValueAsInt("ID"),
                    NeuronType.valueOf(it.getAttributeValueAsString("Type"))
                )

                neurons.add(neuron)
                neuronMap[neuron.id] = neuron
            }

            // Sort the neurons by 1. Type (Input --> ... --> Output) and 2. ID (ascending).
            neurons.sortWith(Comparator { a, b ->
                if (a.type != b.type) {
                    return@Comparator a.type.compareTo(b.type)
                }

                return@Comparator a.id.compareTo(b.id)
            })

            // Load the links.
            val xLinks = xElement.getChild("Links")!!
            xLinks.getChildren("Link").forEach { xLink ->
                val from = xLink.getAttributeValueAsInt("From")
                val to = xLink.getAttributeValueAsInt("To")
                val weight = xLink.getAttributeValueAsDouble("Weight")

                val link = Link(weight, neuronMap[from]!!, neuronMap[to]!!)
                neuronMap[from]!!.outgoing.add(link)
                neuronMap[to]!!.incoming.add(link)
            }

            return NetworkPhenotype(id, neurons, NetworkGenotype(-1, InnovationManager(-1, -1, false, RandomProvider.create())))
        }
    }

    /**
     * The list of input neurons.
     */
    private val inputNeurons = neurons.filter { it.type == NeuronType.Input }.sortedBy { it.id }

    /**
     * The list of output neurons.
     */
    private val outputNeurons = neurons.filter { it.type == NeuronType.Output }.sortedBy { it.id }

    /**
     * The activation function applied for all neurons. TODO: In future work, allow variable activation functions.
     */
    private val activationFunction = ElliotSigmoid()

    /**
     * Creates an output of the network for the input provided.
     *
     * @param input The input to the network.
     * @return The output of the network.
     */
    fun update(input: DoubleArray): Array<Double> {
        return update(Array(input.size) { i -> input[i] })
    }

    /**
     * Creates an output of the network for the input provided. Applies the procedure implemented by Stanley.
     *
     * Source: Stanley, Kenneth Owen. Efficient evolution of neural networks through complexification. Diss. 2004.
     *
     * @param input The input to the network.
     * @return The output of the network.
     */
    fun update(input: Array<Double>): Array<Double> {
        // Copy input values.
        for (index in inputNeurons.indices) {
            inputNeurons[index].setInputValue(input[index])
        }

        var oneTime = false
        var abortCount = 0

        while (outputsOff() || !oneTime) {
            if (++abortCount >= 30) {
                println("INPUTS DISCONNECTED FROM OUTPUT")
            }

            // Compute the sum of incoming activation for all non-input links.
            for (neuron in neurons) {
                if (neuron.type != NeuronType.Input) {
                    neuron.activeSum = 0.0
                    neuron.activeFlag = false


                    for (link in neuron.incoming) {
                        if (link.timeDelay) {
                            neuron.activeSum += link.from.getActiveOutTimeDelay()
                        } else {
                            if (link.from.activeFlag || link.from.type == NeuronType.Input) {
                                neuron.activeFlag = true
                            }

                            neuron.activeSum += link.weight * link.from.getActiveOut()
                        }
                    }
                }
            }

            for (neuron in neurons) {
                // If any active input reached the neuron, determine its activation
                if (neuron.type != NeuronType.Input && neuron.activeFlag) {
                    neuron.lastActivation2 = neuron.lastActivation
                    neuron.lastActivation = neuron.activation

                    //The activation function applied for all neurons. TODO: In future work, allow variable activation functions.
                    neuron.activation = activationFunction.computeY(neuron.activeSum)
                    neuron.activationCount++
                }
            }

            oneTime = true
        }

        return Array(outputNeurons.size) { index -> outputNeurons[index].activation }
    }

//    fun updateOLD(input: Array<Double>): Array<Double> {
//        // Copy input values.
//        for (index in inputNeurons.indices) {
//            inputNeurons[index].setInputValue(input[index])
//        }
//
//        val iterations = 1
//
//        for (iteration in 0 until iterations)
//        {
//            for (neuron in neurons) {
//                if (neuron.type == NeuronType.Hidden || neuron.type ==NeuronType.Output)
//                {
//                    var sum = 0.0
//
//                    for (link in neuron.incoming) {
//                        val neuronOutput = link.from.getActiveOut()
//                        sum+= link.weight * neuronOutput
//                    }
//                    neuron.activationCount = 1
//                    neuron.activation = activationFunction.computeY(sum)
//                }
//            }
//        }
//
//        return Array(outputNeurons.size) { index -> outputNeurons[index].activation }
//    }

    /**
     * Returns true, if any of the output neurons has not been activated during the current update.
     *
     * @return True, if at least one output neuron has not been activated yet.
     */
    fun outputsOff(): Boolean {
        return outputNeurons.any { it.activationCount == 0 }
    }

    override fun copy(): NetworkPhenotype {
        val links = mutableListOf<Link>()

        for (neuron in neurons) {
            // Find all links. The NetworkPhenotype instance does not hold information about that.
            for (link in neuron.outgoing) {
                links.add(link)
            }
        }

        // Copy the neurons.
        val neuronCopies = neurons.map { it.copy() }.toList()
        val neuronMap = hashMapOf<Int, Neuron>()
        neuronCopies.forEach { neuronMap[it.id] = it }

        // Copy the links and connect them to the corresponding neuron-copies.
        for (link in links) {
            val linkCopy = Link(link.weight, neuronMap[link.from.id]!!, neuronMap[link.to.id]!!)

            linkCopy.from.outgoing.add(linkCopy)
            linkCopy.to.incoming.add(linkCopy)
        }

        return NetworkPhenotype(id, neuronCopies, genotype)
//        return NetworkPhenotype(id, neuronCopies, genotype as NetworkGenotype)
    }

    override fun toXElement(): XElement {
//        if (genotype == null) throw Exception("XML export is only available if genotype != null.")

        val xANN = XElement("ANN", XAttribute("ID", id))
        val links = mutableListOf<Link>()

        // Save all neurons.
        val xNeurons = xANN.addChild("Neurons")

        for (neuron in neurons) {
            xNeurons.addChild(neuron.toXElement())

            // Find all links. The NetworkPhenotype instance does not hold information about that.
            for (link in neuron.outgoing) {
                links.add(link)
            }
        }

        // Save all links.
        val xLinks = xANN.addChild("Links", XAttribute("Count", links.size))

        for (link in links) {
            xLinks.addChild(link.toXElement())
        }

        // Save the genotype's fitness.
        xANN.addChild(genotype.fitnessToXElement())

        return xANN
    }
}