package controlparameters

import org.apache.commons.math3.util.Precision

/**
 * Control Parameter.
 *
 * @property scope Determines the possible update interval of the parameter.
 * @property min The absolute minimum value.
 * @property max The absolute maximum value.
 * @property default The absolute default value.
 * @property id Short name of the parameter.
 * @property name_ Long name of the parameter.
 * @property description Description of the application of the parameter.
 * @property precision Number of decimal places the parameter's value is rounded to.
 * @constructor Parameter enum.
 */
enum class Parameter(
    val scope: ParameterScope,
    val min: Double,
    val max: Double,
    val default: Double,
    val id: String,
    val name_: String,
    val description: String,
    val precision: Int = 5
) {

    // User-defined parameters. Unchangeable.
    Population_Size(
        ParameterScope.UserDefined,
        0.0,
        0.0,
        0.0,
        "PS",
        "Population Size",
        "Number of solutions in the population.", 0
    ),
    Num_of_Objectives(
        ParameterScope.UserDefined,
        0.0,
        0.0,
        0.0,
        "NO",
        "Number of Objectives",
        "Number of objectives to optimize simultaneously.",0
    ),
    Max_Evaluations(
        ParameterScope.UserDefined,
        0.0,
        0.0,
        0.0,
        "MT",
        "Number of Evaluations",
        "Maximum number of evaluations before termination.",0
    ),


    // nNEAT:
    // Arbitrary ranges.
    Weight_Mutation_Range(
        ParameterScope.Operation,
        0.1,
        3.5,
        2.5,
        "WMR",
        "Weight Mutation Range",
        "The maximum amount a link's weight can be perturbed within a single mutation operation."
    ),

    // Probabilities.
    Prb_Mutation(
        ParameterScope.Operation,
        0.1,
        0.9,
        0.8,
        "MP",
        "Mutation Probability",
        "Probability that mutation occurs during variation. Inverse of Stanley's \\textit{mate only probability}."
    ),
    Prb_Crossover(
        ParameterScope.Operation,
        0.1,
        0.9,
        0.75,
        "CP",
        "Recombination Probability",
        "Probability that recombination occurs during variation. Inverse of Stanley's \\textit{mutate only probability}."
    ),
    Prb_Cross_Gene_By_Choosing(
        ParameterScope.Operation,
        0.25,
        0.75,
        0.6,
        "MBCP",
        "Mate by Selection Probability",
        "Probability that a common gene is copied from either parent during recombination. Otherwise, the offspring's gene is averaged from both parents."
    ),

    Prb_Modify_Weight(
        ParameterScope.Operation,
        10e-4,
        0.5,
        0.25,
        "MWP",
        "Modify Weight Probability",
        description = "Probability that the weight of a link is perturbed. Multiplied with the number of links of a network to determine the number of weights to mutate."
    ),
    Prb_Add_Neuron(
        ParameterScope.Operation,
        10e-4,
        0.25,
        0.03,
        "ANP",
        "Add Neuron Probability",
        "Probability that a neuron is added to a neural network during mutation."
    ),
    Prb_Add_Link(
        ParameterScope.Operation,
        10e-4,
        0.25,
        0.05,
        "ALP",
        "Add Link Probability",
        "Probability that a link is added to a neural network during mutation."
    ),


    Prb_Gene_Enabled_On_Crossover(
        ParameterScope.Operation,
        10e-4,
        0.1,
        0.001,
        "GEOCP",
        "Gene Enabled on Recombination Prob.",
        description = "Probability that a link is re-enabled during recombination if it occurs in both parents and is disabled in either."
    ),

    Replacement_Rate(
        ParameterScope.Generation,
        0.0,
        0.9,
        0.5,
        "RR",
        "Replacement Rate",
        "Determines the number of offspring created per generation. Multiplied with ${Population_Size.accessByCmd()}, at least one."
    ),
    Selection_Pressure(
        ParameterScope.Generation,
        10e-4,
        1.0,
        0.8,
        "SP",
        "Selection Pressure",
        "Pressure on parent selection. With increasing value, the gap in selection probabilities between worse and better solutions grows."
    ),


    // mNEAT:
    Maximum_Stagnation(
        ParameterScope.Instance,
        15.0,
        1000.0,
        15.0,
        "MS",
        "Maximum Stagnation",
        "Maximum number of generations a species can survive without finding a new best solution among its members. \\textbf{Not applied in nNEAT.}",
        0
    ),
    Prb_Crossover_Interspecies(
        ParameterScope.Generation,
        0.001,
        0.1,
        0.001,
        "IMR",
        "Interspecies Mating Probability",
        "Probability that offspring is created by mating two solutions from distinct species. \\textbf{Not applied in nNEAT.}"
    ),
    Speciation_Coefficient(
        ParameterScope.Generation,
        0.0,
        1.0,
        0.5,
        "SC",
        "Speciation Coefficient",
        "Determines the speciation threshold. Different application according to the speciation procedure. \\textbf{Not applied in nNEAT.}",
        4
    ),

    // Stanley Difference.
    Factor_C1_Excess(
        ParameterScope.Generation,
        0.1,
        1.0,
        1.0,
        "FCE",
        "Factor C1 Excess",
        "Relevance coefficient for excess genes on the difference between two network genomes. \\textbf{Not applied in nNEAT.}"
    ),
    Factor_C2_Disjoint(
        ParameterScope.Generation,
        0.1,
        1.0,
        1.0,
        "FCD",
        "Factor C2 Disjoint",
        "Relevance coefficient for disjoint genes on the difference between two network genomes. \\textbf{Not applied in nNEAT.}"
    ),
    Factor_C3_Weight_Difference(
        ParameterScope.Generation,
        0.1,
        1.0,
        0.4,
        "FCWD",
        "Factor C3 Weight Difference",
        "Relevance coefficient for weight differences of common genes on the total difference between two network genomes. \\textbf{Not applied in nNEAT.}"
    ),


    Prb_Remove_Link(
        ParameterScope.Operation,
        10e-4,
        0.1,
        0.05,
        "RLP",
        "Remove Link Probability",
        "Probability that a link is removed from a neural network during mutation. \\textbf{Not applied in nNEAT. TODO. EXPERIMENTAL.}"
    ),

    // Parameters for further experiments (here: Real-vector representation).
    CROSSOVER_SBX_ETA(ParameterScope.Operation, 1.0, 100.0, 25.0, "CSBX", "Crossover SBX ETA", ""),
    MUTATION_PBX_ETA(ParameterScope.Operation, 1.0, 100.0, 25.0, "MPBX", "Mutation PBX ETA", "")

    ;


    /**
     * Transforms a relative value into an absolute one for the parameter at hand.
     *
     * @param value The relative value v. Note that 0 <= v <= 1.
     * @return
     */
    fun get(value: Double): Double {
        if (!(value in 0.0..1.0)) throw Exception("Invalid relative parameter value: $value")

        return Precision.round(value * (max - min) + min, precision)
    }

    /**
     * Creates a LaTeX command to access the parameter. Only necessary for my dissertation.
     *
     * @return The LaTeX command.
     */
    fun toCmd(): String {
        return "\\newcommand {\\Param$id} {\\Param{$name_}\\xspace}"
    }

    /**
     * Accesses the parmameter using the LaTeX command. Only necessary for my dissertation.
     *
     * @return The parameter access command.
     */
    fun accessByCmd(): String {
        return "\\Param$id"
    }
}