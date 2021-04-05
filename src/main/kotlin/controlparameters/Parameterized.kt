package controlparameters

import java.lang.Exception

/**
 * A parameterized object. For example an EMOA.
 *
 * @constructor Creates a new instance of Parameterized.
 */
open class Parameterized {

    private var initialized = false

    /**
     * Map of registered parameters. Used before initialization.
     */
    private val regParam = mutableMapOf<Parameter, Double>()

    /**
     * Mapping of parameter (enum) to the index its value is stored in _parameters_.
     */
    private var parameterIndices = mapOf<Parameter, Int>()

    /**
     * Parameter value storage.
     */
    private var parameters = Array(0) { 0.5 }


    /**
     * Register a new parameter with given default value. Only registered parameters can be accessed by the inheriting object.
     *
     * @param parameter The parameter to register.
     * @param default The default value (optional).
     */
    fun register(parameter: Parameter, default: Double = 0.5) {
        if (initialized) throw Exception("The Parameterized-object is already initialized. New parameters cannot be registered any more.")

        regParam[parameter] = default
    }

    /**
     * Initializes the Parameterized-object. After executing that method, new parameters cannot be registered any more.
     *
     */
    fun finalizeParameterRegistration() {
        val sortedKeys = regParam.keys.sortedBy { x -> x.ordinal }

        val auxMap = mutableMapOf<Parameter, Int>()
        sortedKeys.forEachIndexed { index, parameter -> auxMap[parameter] = index }

        // Fill the Parameter-Index-Map and set the default values.
        parameterIndices = auxMap.toMap()
        parameters = sortedKeys.map { x -> regParam[x]!! }.toTypedArray()

        initialized = true
    }

    /**
     * Returns a list of all registered parameters for a set of scopes.
     *
     * @param scopes The scopes to consider.
     * @return The registered parameters for the given scopes.
     */
    fun getRegisteredParameters(vararg scopes: ParameterScope): List<Parameter> {
        return parameterIndices.keys.filter { scopes.isEmpty() || scopes.contains(it.scope) }.toList()
    }

    /**
     * Returns the parameter value array.
     *
     * @return The parameter value array.
     */
    fun getParameterArray(): Array<Double> {
        return parameters
    }

    /**
     * Returns a parameter value as integer. Use only for parameters that are actually represented by an integer.
     *
     * @param parameter The parameter to query.
     * @return The value casted to an integer.
     */
    fun getAsInt(parameter: Parameter): Int {
        if (parameter.precision > 0) throw Exception("The actual parameter value is not an Int.")

        return get(parameter).toInt()
    }

    /**
     * Returns a parameter value.
     *
     * @param parameter The parameter to query.
     * @return The value of the parameter.
     */
    open fun get(parameter: Parameter): Double {
        if (!initialized) throw Exception("The Parameterized-object is not initialized. No parameter values can be queried yet.")

        if (!parameterIndices.containsKey(parameter)) {
            throw Exception("The parameter ${parameter.name} has not been registered yet.")
        }

        val value = parameters[parameterIndices[parameter]!!]

        if (parameter.scope == ParameterScope.UserDefined) {
            // As user-defined parameters are stored as absolute values, return that value.
            return value
        }

        // Return the transformed (relative -> absolute) parameter value.
        return parameter.get(value)
    }

    /**
     * Set an integer parameter value. If user-defined: Absolute, otherwise: Relative within [0, 1].
     *
     * @param parameter The parameter to set.
     * @param value The Int value.
     */
    fun set(parameter: Parameter, value: Int) {
        set(parameter, value.toDouble())
    }

    /**
     * Set a double parameter value. If user defined: Absolute, otherwise: Relative within [0, 1].
     *
     * @param parameter The parameter to set.
     * @param value The Double value.
     */
    fun set(parameter: Parameter, value: Double) {
        if (!initialized) throw Exception("The Parameterized-object is not initialized. No parameter values can be queried yet.")

        assert(parameter.scope == ParameterScope.UserDefined || value in 0.0..1.0)

        parameters[parameterIndices[parameter]!!] = value
    }
}