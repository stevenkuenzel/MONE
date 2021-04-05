package elements.genotype.neuralnetworks

import elements.innovation.Innovation
import elements.innovation.InnovationManager
import elements.innovation.NeuronType
import elements.phenotype.neuralnetworks.Link
import elements.phenotype.neuralnetworks.NetworkPhenotype
import elements.phenotype.neuralnetworks.Neuron
import org.apache.commons.math3.util.FastMath
import util.Selection
import de.stevenkuenzel.xml.XAttribute
import de.stevenkuenzel.xml.XElement
import de.stevenkuenzel.xml.XLoadable
import elements.genotype.Genotype

/**
 * The neural network genotype (Stanley's approach).
 *
 * Source:
 *
 * @property innovationManager The innovation manager of the NEAT instance.
 * @constructor Creates a new instance of NetworkGenotype.
 *
 * @param id The ID of the network.
 */
class NetworkGenotype(id: Int, private val innovationManager: InnovationManager) : Genotype<NetworkGenotype>(id) {
    companion object : XLoadable<NetworkGenotype> {
        /**
         * Only for loading and evaluating experiment results.
         */
        override fun fromXElement(xElement: XElement, vararg optional: Any): NetworkGenotype {
            val innovationManager = optional[0] as InnovationManager
            val id = xElement.getAttributeValueAsInt("ID")

            val network = NetworkGenotype(id, innovationManager)
            network.dominatedAfterEvaluations = xElement.getAttributeValueAsInt("DominatedAfterT")
            network.experimentID = xElement.getAttributeValueAsInt("ExperimentID")
            network.generation = xElement.getAttributeValueAsInt("Generation")


            val fitnessAttributes = xElement.getChild("Fitness")!!.attributes
            network.fitness = Array(fitnessAttributes.size) { -1.0 }

            fitnessAttributes.keys.forEach {
                val index = it.substring(1).toInt() - 1

                network.fitness!![index] = fitnessAttributes[it]!!.getValueAs<Double>()!!
            }

            val xGenome = xElement.getChild("Genome")!!
            xGenome.getChildren("Gene").forEach { network.links.add(Gene.fromXElement(it)) }

            return network
        }
    }

    /**
     * List of all linkes, i.e. genes of the the network.
     */
    var links = mutableListOf<Gene>()

    /**
     * True, if no more links can be added.
     */
    var noLinkAnchorsRemaining = false

    /**
     * Constructor for creation of random minimal ANNs.
     */
    constructor(
        innovationManager: InnovationManager,
        id: Int,
        numOfInput: Int,
        numOfOutput: Int,
        bias: Boolean,
        weightMutationRange: Double
    ) : this(id, innovationManager) {
        for (i in 0 until numOfInput) {
            for (j in 0 until numOfOutput) {
                val innovationID = innovationManager.queryOrCreateInnovation(i, numOfInput + j).innovationID
                links.add(
                    Gene(
                        innovationID,
                        weightMutationRange * ((innovationManager.random.nextDouble() - 0.5) * 2.0),
                        true
                    )
                )
            }
        }

        if (bias) {
            for (i in 0 until numOfOutput) {
                val innovationID = innovationManager.queryOrCreateInnovation(numOfInput + numOfOutput, numOfInput + i).innovationID
                links.add(
                    Gene(
                        innovationID,
                        weightMutationRange * ((innovationManager.random.nextDouble() - 0.5) * 2.0),
                        true
                    )
                )
            }
        }

        links.sortBy { x -> x.innovationID }
    }

    /**
     * Constructor for creation of a genome instance after recombination.
     */
    constructor(
        innovationManager: InnovationManager,
        id: Int = -1,
        links_: List<Gene>,
        requiresMutation: Boolean
    ) : this(id, innovationManager) {
        links.addAll(links_)

        links.sortBy { x -> x.innovationID }

        this.requiresMutation = requiresMutation
    }

    override fun toXElement(): XElement {
        val xGenome = XElement("Genome")

        for (link in links) {
            xGenome.addChild("Gene", XAttribute("Innovation", link.innovationID), XAttribute("Weight", link.weight))
        }

        return xGenome
    }

    /**
     * Adds a new link to the network. Returns true, if a link was added.
     */
    private fun addLink(weightMutationRange: Double): Boolean {
        if (noLinkAnchorsRemaining) {
            return false
        }

        val neurons_ = mutableSetOf<Int>()

        val existingPairs = mutableListOf<Pair<Int, Int>>()

        // Find all neurons and existing links.
        links.filter { x -> x.enabled }.forEach { y ->
            val innovation = innovationManager.getInnovation(y.innovationID)
            neurons_.add(innovation.fromNeuronID)
            neurons_.add(innovation.toNeuronID)

            existingPairs.add(Pair(innovation.fromNeuronID, innovation.toNeuronID))
        }

        // Copy the set into a list.
        val neurons = neurons_.toList()

        val endNeurons = neurons.filter {
            val type = innovationManager.getNeuronType(it)
            return@filter type == NeuronType.Hidden || type == NeuronType.Output
        }

        // Find pairs of neurons not linked yet.
        val pairs = mutableListOf<Pair<Int, Int>>()

        neurons.forEach { s ->
            endNeurons.forEach { e ->
                if (existingPairs.none { it.first == s && it.second == e }) {

                    pairs.add(Pair(s, e))
                }
            }
        }

        if (pairs.isEmpty()) {
            // No more "free" links available.
            noLinkAnchorsRemaining = true
            return false
        }

        // Select a pair to link.
        val pair = pairs[innovationManager.random.nextInt(pairs.size)]
        val fromNeuronID = pair.first
        val toNeuronID = pair.second

        // Check if the innovation exists and create it, otherwise.
        val innovation = innovationManager.queryOrCreateInnovation(fromNeuronID, toNeuronID)

        // Add the gene.
        val linkGene = Gene(
            innovation.innovationID,
            weightMutationRange * ((innovationManager.random.nextDouble() - 0.5) * 2.0),
            true
        )

        if (!links.contains(linkGene)) {
            links.add(linkGene)

            return true
        }

        return false
    }


    /**
     * Adds a new neuron to the network. Returns true, if a neuron was added.
     */
    private fun addNeuron(): Boolean {

        // Find links that can be split by the new neuron.
        val splittableLinks = mutableListOf<Pair<Gene, Innovation>>()

        for (link in links) {
            if (!link.enabled) {
                continue
            }

            val innovation = innovationManager.getInnovation(link.innovationID)

            if ((innovation.fromNeuronID != innovation.toNeuronID) && innovationManager.getNeuronType(innovation.fromNeuronID) != NeuronType.Bias) {
                splittableLinks.add(Pair(link, innovation))
            }
        }

        if (splittableLinks.isEmpty()) {
            // No links found.
            return false
        }

        // Select a link and the corresponding neurons.
        val index = innovationManager.random.nextInt(splittableLinks.size)
        val geneInnovation = splittableLinks[index].second
        val gene = splittableLinks[index].first

        val link1: Innovation?
        val link2: Innovation?

        // Check, whether that innovation already exists.
        if (geneInnovation.splitNeuronID != -1) {
            // Link has already been split somewhere else.
            link1 = innovationManager.queryOrCreateInnovation(geneInnovation.fromNeuronID, geneInnovation.splitNeuronID)
            link2 = innovationManager.queryOrCreateInnovation(geneInnovation.splitNeuronID, geneInnovation.toNeuronID)

        } else {
            // The innovation occurs the first time. Set the splitting neuron's id to the innovation.
            val newNeuronID = innovationManager.nextNeuronID++
            geneInnovation.splitNeuronID = newNeuronID

            link1 = innovationManager.queryOrCreateInnovation(geneInnovation.fromNeuronID, newNeuronID)
            link2 = innovationManager.queryOrCreateInnovation(newNeuronID, geneInnovation.toNeuronID)

            // Update the x-coordinate for visualization.
            innovationManager.neuronInfoMap[newNeuronID]!!.splitX =
                (innovationManager.neuronInfoMap[geneInnovation.fromNeuronID]!!.splitX +
                        innovationManager.neuronInfoMap[geneInnovation.toNeuronID]!!.splitX) / 2.0
        }

        // Update depth for later update-calls of the network.
        link1.depth = geneInnovation.depth + 1
        link2.depth = geneInnovation.depth + 1

        // Add the two new links. Disable the old one.
        links.add(Gene(link1.innovationID, 1.0, true))
        links.add(Gene(link2.innovationID, gene.weight, true))
        gene.enabled = false

        noLinkAnchorsRemaining = false

        return true
    }


    /**
     * Perturbs the link weights of the network.
     */
    private fun perturbWeights(forcePerturbation: Boolean, prbModifyWeight: Double, weightMutationRange: Double) {

        // Determine the number of links to perturb.
        var numOfLinks =
            FastMath.round(links.size.toDouble() * prbModifyWeight)
                .toInt()

        if (forcePerturbation && numOfLinks == 0) numOfLinks = 1

        // Select the links to perturb randomly with SUS and equal probabilities.
        val linksToPerturb =
            Selection.selectIndices(Selection.equalDistribution(links.size), numOfLinks, innovationManager.random)

        // Shift the corresponding links' weights randomly.
        for (i in linksToPerturb) {
            val link = links[i]
            val shift = (innovationManager.random.nextDouble() - 0.5) * 2.0 * weightMutationRange

            link.weight += shift
        }
    }

    /**
     * Determines the more fit of two (this instance and _dad_) network genomes and thereby, which genome is allowed to inherit its disjoint and excess genes.
     *
     * @param dad The other network genome.
     * @return True, if this instance is preferred over _dad_. False, otherwise.
     */
    private fun selectDisjointAndExcessProvider(dad: NetworkGenotype): Boolean {
        // Determine more fit parent.
        var disjointAndExcessFromMum = false

        when {
            qValue > dad.qValue -> {
                disjointAndExcessFromMum = true
            }
            dad.qValue > qValue -> {
                disjointAndExcessFromMum = false
            }
            qValue == dad.qValue -> {
                disjointAndExcessFromMum = when {
                    links.size < dad.links.size -> {
                        true
                    }
                    dad.links.size < links.size -> {
                        false
                    }
                    else -> {
                        innovationManager.random.nextBoolean()
                    }
                }
            }
        }

        return disjointAndExcessFromMum
    }


    override fun toPhenotype(): NetworkPhenotype {
        val neuronIDs = mutableSetOf<Int>()

        for (link in links) {
            if (!link.enabled) {
                continue
            }
            val innovation = innovationManager.getInnovation(link.innovationID)

            neuronIDs.add(innovation.fromNeuronID)
            neuronIDs.add(innovation.toNeuronID)
        }

        val neurons = mutableListOf<Neuron>()
        val neuronMap = hashMapOf<Int, Neuron>()

        for (neuronID in neuronIDs) {
            val neuron = Neuron(neuronID, innovationManager.getNeuronType(neuronID))
            neurons.add(neuron)
            neuronMap[neuronID] = neuron
        }

        // Sort the neurons by 1. Type (Input --> ... --> Output) and 2. ID (ascending).
        neurons.sortWith(Comparator { a, b ->
            if (a.type != b.type) {
                return@Comparator a.type.compareTo(b.type)
            }

            return@Comparator a.id.compareTo(b.id)
        })

        for (link in links) {
            if (!link.enabled) {
                continue
            }

            val innovation = innovationManager.getInnovation(link.innovationID)

            val linkInstance =
                Link(link.weight, neuronMap[innovation.fromNeuronID]!!, neuronMap[innovation.toNeuronID]!!)
            neuronMap[innovation.fromNeuronID]!!.outgoing.add(linkInstance)
            neuronMap[innovation.toNeuronID]!!.incoming.add(linkInstance)
        }

        return NetworkPhenotype(id, neurons, this)
    }

    override fun mutate(vararg args: Any) {
        val prbAddLink = args[0] as Double
        val prbAddNeuron = args[1] as Double
        val prbModifyWeight = args[2] as Double
        val weightMutationRange = args[3] as Double

        val addedLink = innovationManager.random.nextDouble() <= prbAddLink && addLink(weightMutationRange)
        val addedNeuron = innovationManager.random.nextDouble() <= prbAddNeuron && addNeuron()

        // If none of the above operations was carried out, force at least the perturbation of a single link weight to avoid an exact copy.
        val genomeChanged = addedLink || addedNeuron
        perturbWeights(!genomeChanged, prbModifyWeight, weightMutationRange)

        if (genomeChanged) {
            links.sortBy { x -> x.innovationID }
        }
    }

    /**
     * NOTE: THIS IMPLEMENTATION DIFFERS FROM THE ONE APPLIED FOR THE EXPERIMENTS IN MY DISSERTATION.
     */
    override fun crossWith(other: NetworkGenotype, vararg args: Any): NetworkGenotype {
        val prbCrossGeneByChoosing = args[0] as Double
        val prbGeneEnabledOnCrossover = args[1] as Double
        val newID = args[2] as Int

        val offspringGenes = mutableListOf<Gene>()

        // Determine more fit parent.
        val disjointAndExcessFromMum = selectDisjointAndExcessProvider(other)

        var numCommonTakenFromMum = 0
        var numCommonTakenFromDad = 0

        var indexMum = 0
        var indexDad = 0


        // Iterate over both genomes.
        while (!(indexMum == links.size && indexDad == other.links.size)) {
            var geneToAdd: Gene? = null


            // End of mum's genes has been reached. (EXCESS)
            if (indexMum == links.size && indexDad < other.links.size) {
                if (!disjointAndExcessFromMum) {
                    geneToAdd = other.links[indexDad].copy()
                }

                indexDad++
            }
            // End of dad's genes has been reached. (EXCESS)
            else if (indexDad == other.links.size && indexMum < links.size) {
                if (disjointAndExcessFromMum) {
                    geneToAdd = links[indexMum].copy()
                }

                indexMum++
            }
            // Is mum's innovation id less than dad's? (DISJOINT)
            else if (links[indexMum].innovationID < other.links[indexDad].innovationID) {
                if (disjointAndExcessFromMum) {
                    geneToAdd = links[indexMum].copy()
                }

                indexMum++
            }
            // Is dad's innovation id less than mum's? (DISJOINT)
            else if (other.links[indexDad].innovationID < links[indexMum].innovationID) {

                if (!disjointAndExcessFromMum) {
                    geneToAdd = other.links[indexDad].copy()
                }

                indexDad++
            }
            // Equal innovation ids. Select a random parent's gene. (COMMON)
            else if (links[indexMum].innovationID == other.links[indexDad].innovationID) {

                geneToAdd = Gene(links[indexMum].innovationID, 0.0, true)


                if (links[indexMum].weight != other.links[indexDad].weight) {
                    val mateByChoosing = innovationManager.random.nextDouble() <= prbCrossGeneByChoosing

                    if (mateByChoosing) {
                        val takeMumsGene = innovationManager.random.nextDouble() <= 0.5

                        if (takeMumsGene) {
                            numCommonTakenFromMum++
                            geneToAdd.weight = links[indexMum].weight
                        } else {
                            numCommonTakenFromDad++
                            geneToAdd.weight = other.links[indexDad].weight
                        }
                    } else {
                        numCommonTakenFromMum++
                        numCommonTakenFromDad++
                        geneToAdd.weight = (links[indexMum].weight + other.links[indexDad].weight) / 2.0
                    }
                } else {
                    geneToAdd.weight = links[indexMum].weight
                }

                geneToAdd.enabled =
                    if (disjointAndExcessFromMum) links[indexMum].enabled else other.links[indexDad].enabled

                val disabledInEither = links[indexMum].enabled xor other.links[indexDad].enabled

                if (disabledInEither && innovationManager.random.nextDouble() <= prbGeneEnabledOnCrossover) {
                    geneToAdd.enabled = true
                }

                indexMum++
                indexDad++
            }

            if (geneToAdd != null) {
                offspringGenes.add(geneToAdd)
            }
        }

        // Create the new child. Set the mutation flag, if either mum or dad did not contribute to avoid an exact copy.
        return NetworkGenotype(
            innovationManager,
            newID,
            offspringGenes,
            numCommonTakenFromDad == 0 || numCommonTakenFromMum == 0
        )
    }

    override fun copy(vararg args: Any): NetworkGenotype {
        val newID = args[0] as Int
        return NetworkGenotype(innovationManager, newID, links.map { it.copy() }.toList(), false)
    }


    override fun getGenomeSize(): Int {
        return links.size
    }

    override fun getStructureInformation(): Array<Double> {
        val neuronIDs = mutableSetOf<Int>()
        var recurrentLinks = 0

        for (link in links) {
            if (!link.enabled) continue

            val innovation = innovationManager.getInnovation(link.innovationID)

            neuronIDs.add(innovation.fromNeuronID)
            neuronIDs.add(innovation.toNeuronID)

            if (innovationManager.neuronInfoMap[innovation.fromNeuronID]!!.splitX >=
                innovationManager.neuronInfoMap[innovation.toNeuronID]!!.splitX
            ) recurrentLinks++
        }

        return arrayOf(neuronIDs.size.toDouble(), links.size.toDouble(), recurrentLinks.toDouble())
    }

    override fun getStructureLabels(): Array<String> {
        return arrayOf("Neurons", "Links", "Links (Rec.)")
    }
}