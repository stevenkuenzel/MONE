package experiments

/**
 * Contains the fitness vector of a phenotype against a certain reference.
 *
 * @property phenotypeID The phenotype ID.
 * @property referenceID The reference ID.
 * @property objectives The fitness vector.
 * @constructor Creates a new instance.
 */
data class SampleVector(val phenotypeID : Int, val referenceID : Int, val objectives : Array<Double>)