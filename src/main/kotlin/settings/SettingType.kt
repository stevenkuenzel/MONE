package settings

import kotlinx.serialization.Serializable

/**
 * Setting type. Specified by the type of the value that is stored in a setting.
 */
@Serializable
enum class SettingType {
    Boolean,
    Double,
    Int,
    Long,
    String
}