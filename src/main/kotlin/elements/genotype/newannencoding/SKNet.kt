package elements.genotype.newannencoding

import de.stevenkuenzel.xml.XAttribute
import de.stevenkuenzel.xml.XElement
import de.stevenkuenzel.xml.XLoadable
import elements.genotype.Genotype
import elements.genotype.neuralnetworks.Gene
import elements.genotype.neuralnetworks.NetworkGenotype
import elements.innovation.InnovationManager
import elements.innovation.NeuronType
import elements.phenotype.neuralnetworks.Link
import elements.phenotype.neuralnetworks.NetworkPhenotype
import elements.phenotype.neuralnetworks.Neuron
import util.random.RandomProvider
import kotlin.math.sqrt

class SKNet(id: Int, val numInput: Int, val numOutput: Int) : Genotype<SKNet>(id) {
    companion object : XLoadable<SKNet> {
        fun cantor(x: Int, y: Int): Int {
            return ((x + y) * (x + y + 1)) / 2 + y
        }

        fun cantorInv(z: Int): Pair<Int, Int> {
            val w = (sqrt((8 * z + 1).toDouble()).toInt() - 1) / 2
            val t = (w * w + w) / 2

            val y = z - t
            val x = w - y

            return Pair(x, y)
        }

        /**
         * Only for loading and evaluating experiment results.
         */
        override fun fromXElement(xElement: XElement, vararg optional: Any): SKNet {
            val id = xElement.getAttributeValueAsInt("ID")

            val network = SKNet(id, 3, 1)
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
            xGenome.getChildren("Gene").forEach { network.links.add(SKLink.fromXElement(it)) }

            return network
        }
    }


    val random = RandomProvider.create()
    val links = mutableListOf<SKLink>()

    constructor(id: Int, numInput: Int, numOutput: Int, links_: List<SKLink>) : this(id, numInput, numOutput) {
        for (lnk_ in links_) {
            links.add(lnk_)
        }
    }


    fun createBase(weightMutationRange: Double) {
        for (i in 0 until numInput) {
            for (j in 0 until numOutput) {
                links.add(SKLink(i, numInput + j, (random.nextDouble() - 0.5) * 2.0 * weightMutationRange))
            }
        }
    }

    fun addLink(weightMutationRange: Double): Boolean {
        val availFrom = mutableSetOf<Int>()
        val availTo = mutableSetOf<Int>()
        links.forEach {
            if (it.from >= numInput) availTo.add(it.from)
            if (it.to >= numInput) availTo.add(it.to)

            availFrom.add(it.from)
            availFrom.add(it.to)
        }

        val avail = mutableListOf<Pair<Int, Int>>()

        for (s in availFrom) {
            for (e in availTo) {
                val linkID = cantor(s, e)

                if (links.none { it.linkID == linkID }) avail.add(Pair(s, e))
            }
        }

        if (avail.isEmpty()) return false

        val sel = avail.random()

        links.add(SKLink(sel.first, sel.second, (random.nextDouble() - 0.5) * 2.0 * weightMutationRange))

        return true
    }

    fun splitLink(): Boolean {
        // Filter links. No self-recurrent.

        val avail = links.filter { it.from != it.to }
        val selected = avail.random()

        val neuron = selected.linkID

        links.remove(selected)

        links.add(SKLink(selected.from, neuron, random.nextDouble()))
        links.add(SKLink(neuron, selected.to, 1.0))

        return true
    }

    fun removeLink(): Boolean {
        val avail = mutableListOf<SKLink>()

        /*
        Conditions:
        - There is at least one other link that starts at the link's starting neuron. The link must not be self-recurrent.
        - There is at least one other link that end at the link's end neuron. The link must not be self-recurrent.
        OR:
        - The link is self-recurrent.
         */

        for (i in links.indices) {
            val link = links[i]

            // Condition: Self-recurrent.
            if (link.from == link.to) {
                avail.add(link)
                continue
            }

            var conditionStart = false
            var conditionEnd = false

            for (j in links.indices) {
                if (i == j) continue

                val other = links[j]

                // The other link must not be self-recurrent.
                if (other.from == other.to) continue


                if (link.from == other.from) conditionStart = true
                if (link.to == other.to) conditionEnd = true

                if (conditionStart && conditionEnd) {
                    avail.add(link)
                    break
                }
            }
        }
        if (avail.isEmpty()) return false

        val selected = avail.random()
        links.remove(selected)

        return true
    }

    fun perturbWeights(prbModifyWeight: Double, weightMutationRange: Double) {
        for (link in links) {
            if (random.nextDouble() <= prbModifyWeight) {
                val newWeight = link.weight + (random.nextDouble() - 0.5) * 2.0 * weightMutationRange
                link.weight = newWeight
            }
        }
    }

    override fun toXElement(): XElement {
        val xElement = XElement("Genome", XAttribute("ID", id))

        for (link in links) {
            xElement.add(link.toXElement())
        }

        return xElement
    }

    override fun mutate(vararg args: Any) {
        val prbAddLink = args[0] as Double
        val prbAddNeuron = args[1] as Double
        val prbRemLink = args[2] as Double
        val prbModifyWeight = args[3] as Double
        val weightMutationRange = args[4] as Double

        val addedLink = random.nextDouble() <= prbAddLink && addLink(weightMutationRange)
        val addedNeuron = random.nextDouble() <= prbAddNeuron && splitLink()
        val remLnk = random.nextDouble() <= prbRemLink && removeLink()

        // If none of the above operations was carried out, force at least the perturbation of a single link weight to avoid an exact copy.
        val genomeChanged = addedLink || addedNeuron || remLnk
        perturbWeights(prbModifyWeight, weightMutationRange)

        if (genomeChanged) {
            links.sortBy { it.linkID }
        }
    }

    data class RecombinationGene(val link: SKLink, var value1: Double?, var value2: Double?) {
        override fun equals(other: Any?): Boolean {
            if (other is RecombinationGene) {
                return link.linkID == other.link.linkID
            }

            return false
        }

        override fun hashCode(): Int {
            return link.linkID
        }
    }

    enum class InequalGeneMode {
        Parent1,
        Parent2,
        Both
    }

    override fun crossWith(other: SKNet, vararg args: Any): SKNet {
        val prbCrossGeneByChoosing = args[0] as Double
        val newID = args[1] as Int
        // Parents a, b. Take either:
        // Common: Just as NEAT.
        // Disjoint + Excess: Either a or b. Or Both.

        val geneSet = hashMapOf<Int, RecombinationGene>()

        for (link in links) {
            geneSet[link.linkID] = RecombinationGene(link, link.weight, null)
        }

        for (link in other.links) {
            if (geneSet.containsKey(link.linkID)) {
                geneSet[link.linkID]!!.value2 = link.weight
            } else {
                geneSet[link.linkID] = RecombinationGene(link, null, link.weight)
            }
        }

        // Determine the inqueal mode.
        val inequalMode = when {
            qValue > other.qValue -> InequalGeneMode.Parent1
            other.qValue > qValue -> InequalGeneMode.Parent2
            else -> InequalGeneMode.Both
        }

        val offspringGenes = mutableListOf<SKLink>()

        for (key in geneSet.keys) {
            val rcg = geneSet[key]!!

            if (rcg.value1 != null && rcg.value2 != null) {
                // Common gene.
                val mateByChoosing = random.nextDouble() <= prbCrossGeneByChoosing
                if (mateByChoosing) {
                    val takeMumsGene = random.nextDouble() <= 0.5

                    if (takeMumsGene) {
                        offspringGenes.add(SKLink(rcg.link.from, rcg.link.to, rcg.value1!!))
                    } else {
                        offspringGenes.add(SKLink(rcg.link.from, rcg.link.to, rcg.value2!!))
                    }
                } else {
                    offspringGenes.add(SKLink(rcg.link.from, rcg.link.to, (rcg.value1!! + rcg.value2!!) / 2.0))
                }
            } else {
                val value = (if (rcg.value1 != null && inequalMode != InequalGeneMode.Parent2) {
                    rcg.value1
                } else if (rcg.value2 != null && inequalMode != InequalGeneMode.Parent1){
                    rcg.value2
                })!!
                else
                {

                }

                offspringGenes.add(SKLink(rcg.link.from, rcg.link.to, value))
            }
        }


//                when (inequalMode) {
//                    InequalGeneMode.Parent1 -> if (rcg.value1 != null) offspringGenes.add(
//                        SKLink(
//                            rcg.link.from,
//                            rcg.link.to,
//                            rcg.value1!!
//                        )
//                    )
//                    InequalGeneMode.Parent2 -> if (rcg.value2 != null) offspringGenes.add(
//                        SKLink(
//                            rcg.link.from,
//                            rcg.link.to,
//                            rcg.value2!!
//                        )
//                    )
//                    InequalGeneMode.Both -> if (rcg.value1 != null) {
//                        offspringGenes.add(SKLink(rcg.link.from, rcg.link.to, rcg.value1!!))
//                    } else {
//                        offspringGenes.add(SKLink(rcg.link.from, rcg.link.to, rcg.value2!!))
//                    }
//                }
//            }
//        }


        // Create the new child. Set the mutation flag, if either mum or dad did not contribute to avoid an exact copy.
        return SKNet(newID, numInput, numOutput, offspringGenes)
    }

    fun crossWithLEGACY(other: SKNet, vararg args: Any): SKNet {
        val prbCrossGeneByChoosing = args[0] as Double
        val newID = args[1] as Int

        val offspringGenes = mutableListOf<SKLink>()

        // Determine more fit parent.
        val disjointAndExcessFromMum = qValue > other.qValue

        var numCommonTakenFromMum = 0
        var numCommonTakenFromDad = 0

        var indexMum = 0
        var indexDad = 0


        // Iterate over both genomes.
        while (!(indexMum == links.size && indexDad == other.links.size)) {
            var geneToAdd: SKLink? = null


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
            else if (links[indexMum].linkID < other.links[indexDad].linkID) {
                if (disjointAndExcessFromMum) {
                    geneToAdd = links[indexMum].copy()
                }

                indexMum++
            }
            // Is dad's innovation id less than mum's? (DISJOINT)
            else if (other.links[indexDad].linkID < links[indexMum].linkID) {

                if (!disjointAndExcessFromMum) {
                    geneToAdd = other.links[indexDad].copy()
                }

                indexDad++
            }
            // Equal innovation ids. Select a random parent's gene. (COMMON)
            else if (links[indexMum].linkID == other.links[indexDad].linkID) {

                geneToAdd = SKLink(links[indexMum].from, links[indexMum].to, 0.0)

                if (links[indexMum].weight != other.links[indexDad].weight) {
                    val mateByChoosing = random.nextDouble() <= prbCrossGeneByChoosing

                    if (mateByChoosing) {
                        val takeMumsGene = random.nextDouble() <= 0.5

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

                indexMum++
                indexDad++
            }

            if (geneToAdd != null) {
                offspringGenes.add(geneToAdd)
            }
        }

        // Create the new child. Set the mutation flag, if either mum or dad did not contribute to avoid an exact copy.
        return SKNet(newID, numInput, numOutput, offspringGenes)
    }

    override fun copy(vararg args: Any): SKNet {

        val newID = args[0] as Int
        return SKNet(newID, numInput, numOutput, links.map { it.copy() })
    }

    override fun toPhenotype(): NetworkPhenotype {
        val neuronIDs = mutableSetOf<Int>()

        for (link in links) {
            neuronIDs.add(link.from)
            neuronIDs.add(link.to)
        }

        val neurons = mutableListOf<Neuron>()
        val neuronMap = hashMapOf<Int, Neuron>()

        for (neuronID in neuronIDs) {
            val neuron = Neuron(
                neuronID, when (neuronID) {
                    in 0 until numInput -> NeuronType.Input
                    in numInput until numInput + numOutput -> NeuronType.Output
                    else -> NeuronType.Hidden
                }
            )
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
            val linkInstance =
                Link(link.weight, neuronMap[link.from]!!, neuronMap[link.to]!!)
            neuronMap[link.from]!!.outgoing.add(linkInstance)
            neuronMap[link.to]!!.incoming.add(linkInstance)
        }

        return NetworkPhenotype(id, neurons, this)
    }

    override fun getGenomeSize(): Int {
        return links.size
    }

    override fun getStructureInformation(): Array<Double> {
        val neuronIDs = mutableSetOf<Int>()

        for (link in links) {
            neuronIDs.add(link.from)
            neuronIDs.add(link.to)
        }

        return arrayOf(neuronIDs.size.toDouble(), links.size.toDouble())
    }

    override fun getStructureLabels(): Array<String> {
        return arrayOf("Neurons", "Links")
    }
}