package controlparameters

/**
 * Scope of a parameter. Determines the possible update-frequency for the parameter controller.
 *
 * @constructor Create empty Parameter scope
 */
enum class ParameterScope {
    /**
     * Update possible after each operation (e.g. variation, mutation).
     */
    Operation,

    /**
     * Update possible after each epoch.
     */
    Generation,

    /**
     * Do not update throughout the evolutionary process.
     */
    Instance,

    /**
     * User-defined value. Not handed to a parameter controller.
     */
    UserDefined
}