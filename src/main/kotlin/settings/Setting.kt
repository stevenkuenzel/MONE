package settings

import de.stevenkuenzel.xml.XElement
import de.stevenkuenzel.xml.XSavable
import kotlinx.serialization.Serializable

/**
 * A serializable user-setting.
 *
 * @property name Name of the setting.
 * @property value Value of the setting.
 * @property replaceBy Name of the setting when it is saved to an experiment result. For settings that can contain multiple values at once, e.g., POPULATION_SIZES=20,50 --> POPULATION_SIZE.
 * @property description Description of the setting.
 * @property type Type of the setting's value.
 * @constructor Creates a new instance.
 */
@Serializable
data class Setting(val name : String, val value : String, val replaceBy : String, val description : String, val type : SettingType)
{
    constructor(name: String, value: String, replaceBy : String =  "", description : String =  "") : this(name, value, replaceBy, description, SettingType.String)
    constructor(name: String, value: Int, replaceBy : String =  "", description : String =  "") : this(name, value.toString(), replaceBy, description, SettingType.Int)
    constructor(name: String, value: Long, replaceBy : String =  "", description : String =  "") : this(name, value.toString(), replaceBy, description, SettingType.Long)
    constructor(name: String, value: Double, replaceBy : String =  "", description : String =  "") : this(name, value.toString(), replaceBy, description, SettingType.Double)
    constructor(name: String, value: Boolean, replaceBy : String =  "", description : String =  "") : this(name, value.toString(), replaceBy, description, SettingType.Boolean)

    // Methods, that return the value of this setting casted to a certain type.

    fun getValueAsString() : String
    {
        return value
    }

    fun getValueAsInt() : Int
    {
        return value.toInt()
    }

    fun getValueAsLong() : Long
    {
        return value.toLong()
    }

    fun getValueAsDouble() : Double
    {
        return value.toDouble()
    }

    fun getValueAsBoolean() : Boolean
    {
        return value.toBoolean()
    }
}