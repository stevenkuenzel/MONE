package postprocessing.statistics

/**
 * Result of a statistical comparison.
 */
enum class TestResult {
    Indifferent, // = No significant difference.
    /*Significantly*/ Smaller,
    /*Significantly*/ Greater
}