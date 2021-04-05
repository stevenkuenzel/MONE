package elements.genotype

import de.stevenkuenzel.xml.XAttribute
import de.stevenkuenzel.xml.XElement
import de.stevenkuenzel.xml.XSavable
import elements.FitnessElement
import elements.phenotype.Phenotype
import org.apache.commons.math3.util.Precision
import kotlin.reflect.KClass

/**
 * An abstract Genotype.
 *
 * @param T Type of the genotype.
 * @constructor Creates a new instance of Genotype.
 *
 * @param id The ID of the genome.
 */
abstract class Genotype<T>(id: Int) : FitnessElement(id), XSavable {


    /**
     * True, if the genome has to be mutated. This occurs if a genome was created by recombination of two solutions and has only received the genes of either parent.
     */
    var requiresMutation = false

    /**
     * Mutates the genotype. Does not return a new instance.
     */
    abstract fun mutate(vararg args: Any)

    /**
     * Recombines the genotype instance with another one. Returns a new child instance.
     */
    abstract fun crossWith(other: T, vararg args: Any): T

    /**
     * Returns a copy of the genotype.
     */
    abstract fun copy(vararg args: Any): T

    /**
     * Creates an XElement of the fitness array.
     *
     * @return The XElement.
     */
    fun fitnessToXElement(): XElement {
        if (fitness == null) throw Exception("Fitness is null. Export to XML not possible.")

        val xFitness = XElement("Fitness")

        for (index in fitness!!.indices) {
            xFitness.addAttribute("f${index + 1}", Precision.round(fitness!![index], 4))
        }

        return xFitness
    }

    /**
     * Returns the fitness as string. Matches Windows file name specifications.
     *
     * @return The fitness string.
     */
    fun getFitnessAsString(): String {
        if (fitness == null) throw Exception("Fitness is null. Export to string not possible.")

        var result = "("

        for (index in fitness!!.indices) {
            result += "${Precision.round(fitness!![index], 2)}, "
        }

        return result.substring(0, result.length - 2) + ")"
    }

    /**
     * Returns an XElement with additional meta information.
     *
     * @return The XElement.
     */
    fun toXElementWithMetaInformation(): XElement {
        val xElement = XElement(
            "Genotype",
            XAttribute("ID", id),
            XAttribute("DominatedAfterT", dominatedAfterEvaluations),
            XAttribute("ExperimentID", experimentID),
            XAttribute("Generation", generation)
        )

        xElement.addChild(toXElement())
        xElement.addChild(fitnessToXElement())

        return xElement
    }

    /**
     * Converts the genome to a phenotype.
     *
     * @return The phenotype.
     */
    abstract fun toPhenotype() : Phenotype


    /**
     * Returns the size of the genome.
     *
     * @return The genome size.
     */
    abstract fun getGenomeSize() : Int

    /**
     * Returns information about the strcture of the genome. For XML-export and evaluation only.
     *
     * @return An array of strucutre information.
     */
    abstract fun getStructureInformation() : Array<Double>

    /**
     * Returns the labels for the structure information returned by _getStructureInformation()_. For XML-export and evaluation only.
     *
     * @return An array of structure information labels.
     */
    abstract fun getStructureLabels() : Array<String>
}