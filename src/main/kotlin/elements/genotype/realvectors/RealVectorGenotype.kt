package elements.genotype.realvectors

import de.stevenkuenzel.xml.XAttribute
import de.stevenkuenzel.xml.XElement
import de.stevenkuenzel.xml.XLoadable
import elements.genotype.Genotype
import elements.phenotype.realvectors.RealVectorPhenotype
import util.random.RandomProvider
import kotlin.math.pow

/**
 * A real vector genotype.
 *
 * @property genes The array containing the genes.
 * @property random A random number generator.
 * @constructor Creates a new instance of RealVectorGenotype.
 *
 * @param id The ID of the element.
 */
class RealVectorGenotype(id: Int, val genes: Array<Double>, val random: RandomProvider) : Genotype<RealVectorGenotype>(id) {
    companion object : XLoadable<RealVectorGenotype> {
        /**
         * Only for loading and evaluating experiment results.
         */
        override fun fromXElement(xElement: XElement, vararg optional: Any): RealVectorGenotype {
            val random = optional[0] as RandomProvider
            val id = xElement.getAttributeValueAsInt("ID")


            val xGenome = xElement.getChild("Genome")!!
            val xGenes = xGenome.getChildren("Gene")
            val genes = Array(xGenes.size) {0.0}
            xGenes.forEach { genes[it.getAttributeValueAsInt("Index")] = it.getAttributeValueAsDouble("Value") }

            val genotype = RealVectorGenotype(id, genes, random)
            genotype.dominatedAfterEvaluations = xElement.getAttributeValueAsInt("DominatedAfterT")
            genotype.experimentID = xElement.getAttributeValueAsInt("ExperimentID")
            genotype.generation = xElement.getAttributeValueAsInt("Generation")


            val fitnessAttributes = xElement.getChild("Fitness")!!.attributes
            genotype.fitness = Array(fitnessAttributes.size) { -1.0 }

            fitnessAttributes.keys.forEach {
                val index = it.substring(1).toInt() - 1

                genotype.fitness!![index] = fitnessAttributes[it]!!.getValueAs<Double>()!!
            }


            return genotype
        }
    }

    /**
     * Polynomial mutatation based on the work of Deb et al.
     *
     * Source: Deb, Kalyanmoy, et al. "A fast and elitist multiobjective genetic algorithm: NSGA-II." IEEE transactions on evolutionary computation 6.2 (2002): 182-197.
     *
     */
    override fun mutate(vararg args: Any) {
        val eta_m = args[0] as Double

        for (index in genes.indices) {
            if (random.nextDouble() <= 0.1) {
                var y = genes[index]
                val yl = 0.0
                val yu = 1.0

                val delta1 = (y - yl) / (yu - yl)
                val delta2 = (yu - y) / (yu - yl)

                val rnd = random.nextDouble()
                val mutPow = 1.0 / (eta_m + 1.0)

                val deltaq = if (rnd <= 0.5) {
                    val xy = 1.0 - delta1
                    val val_ = 2.0 * rnd + (1.0 - 2.0 * rnd) * xy.pow(eta_m + 1.0)

                    val_.pow(mutPow) - 1.0
                } else {
                    val xy = 1.0 - delta2
                    val val_ = 2.0 * (1.0 - rnd) + 2.0 * (rnd - 0.5) * xy.pow(eta_m + 1.0)

                    1.0 - val_.pow(mutPow)
                }

                y += deltaq * (yu - yl)

                y = y.coerceIn(yl, yu)

                genes[index] = y
            }
        }
    }

    /**
     * SBX crossover based on the work of Deb et al. (This implementation returns only a single child.)
     *
     * Source: Deb, Kalyanmoy, et al. "A fast and elitist multiobjective genetic algorithm: NSGA-II." IEEE transactions on evolutionary computation 6.2 (2002): 182-197.
     *
     */
    override fun crossWith(other: RealVectorGenotype, vararg args: Any): RealVectorGenotype {

        val newID = args[0] as Int
        val eta_c = args[1] as Double

        val a = this
        val b = other

        val g1 = Array(a.genes.size) { 0.0 }
//        val g2 = Array(g1.size) { 0.0 }

        for (index in a.genes.indices) {
            if (random.nextDouble() <= 0.5) {
                val y1 = if (a.genes[index] < b.genes[index]) a.genes[index] else b.genes[index]
                val y2 = if (a.genes[index] > b.genes[index]) a.genes[index] else b.genes[index]

                if (y1 != y2) {
                    val yl = 0.0
                    val yu = 1.0

                    val rand = random.nextDouble()
                    var beta = 1.0 + (2.0 * (y1 - yl) / (y2 - y1))
                    var alpha = 2.0 - beta.pow(-(eta_c + 1.0))

                    var betaq = if (rand <= (1.0 / alpha)) {
                        (rand * alpha).pow(1.0 / (eta_c + 1.0))
                    } else {
                        (1.0 / (2.0 - rand * alpha)).pow(1.0 / (eta_c + 1.0))
                    }

                    g1[index] = 0.5 * ((y1 + y2) - betaq * (y2 - y1))

//                    beta = 1.0 + (2.0 * (yu - y2) / (y2 - y1))
//                    alpha = 2.0 - beta.pow(-(eta_c + 1.0))
//
//                    betaq = if (rand <= (1.0 / alpha)) {
//                        (rand * alpha).pow(1.0 / (eta_c + 1.0))
//                    } else {
//                        (1.0 / (2.0 - rand * alpha)).pow(1.0 / (eta_c + 1.0))
//                    }
//
//                    g2[index] = 0.5 * ((y1 + y2) + betaq * (y2 - y1))

                    g1[index] = g1[index].coerceIn(yl, yu)
//                    g2[index] = g2[index].coerceIn(yl, yu)
                } else {
                    g1[index] = a.genes[index]
//                    g2[index] = b.genes[index]
                }
            } else {
                g1[index] = a.genes[index]
//                g2[index] = b.genes[index]
            }
        }

        return RealVectorGenotype(newID, g1, random)
    }

    override fun copy(vararg args: Any): RealVectorGenotype {
        val newID = args[0] as Int
        return RealVectorGenotype(newID, Array(genes.size) { i -> genes[i] }, random)
    }

    override fun toXElement(): XElement {
        val xGenome = XElement("Genome")

        for (index in genes.indices) {
            xGenome.addChild(XElement("Gene", XAttribute("Index", index), XAttribute("Value", genes[index])))
        }

        return xGenome
    }

    override fun toPhenotype(): RealVectorPhenotype {
        return RealVectorPhenotype(id, this)
    }
    override fun getGenomeSize(): Int {
        return genes.size
    }

    override fun getStructureInformation(): Array<Double> {
        return arrayOf(genes.size.toDouble())
    }

    override fun getStructureLabels(): Array<String> {
        return arrayOf("Variables")
    }
}