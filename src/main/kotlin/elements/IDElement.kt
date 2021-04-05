package elements

/**
 * An element with a fixed ID.
 *
 * @property id The ID of the element.
 * @constructor Creates a new instance of IDElement.
 */
open class IDElement(val id: Int) {
    override fun equals(other: Any?): Boolean {
        if (other is IDElement) {
            if (other.javaClass == javaClass) {
                return other.id == id
            }
        }

        return false
    }

    override fun hashCode(): Int {
        return id
    }

    override fun toString(): String {
        return "$id"
    }
}