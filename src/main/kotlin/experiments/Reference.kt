package experiments

import elements.IDElement

/**
 * A reference for evaluating solutions against.
 *
 * @property name The name of the reference.
 * @constructor Creates a new instance.
 *
 * @param id The ID of the reference.
 */
open class Reference(id: Int, val name : String) : IDElement(id)