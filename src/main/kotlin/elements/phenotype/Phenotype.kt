package elements.phenotype

import de.stevenkuenzel.xml.XSavable
import elements.IDElement
import elements.genotype.Genotype

/**
 * An abstract Phenotype.
 *
 * @property genotype The Genotype instance that created this Phenotype instance.
 * @constructor Creates a new instance of Phenotype.
 *
 * @param id The ID of this phenotype.
 */
abstract class Phenotype(id : Int, val genotype: Genotype<out Any>) : IDElement(id), XSavable {
    /**
     * Creates a copy of this instance.
     *
     * @return The copy.
     */
    abstract fun copy() : Phenotype
}