package experiments.fightingice.annbot

import ftginterface.skills.Skill

/**
 * Links a skill with a JPD.
 *
 * @property skill The skill.
 * @property jointProbabilityDistribution The JPD.
 * @constructor Creates a new instance.
 */
data class ANNBotSkill(val skill: Skill, val jointProbabilityDistribution: JointProbabilityDistribution) {
    /**
     * Approximates the value of the skill w.r.t. the distance vector provided and the JPD.
     *
     * @param input The distance vector -- requires two integers: x and y.
     * @return The value of the skill.
     */
    fun approximate(vararg input : Int) : Double
    {
        return jointProbabilityDistribution.getJointProbabilityNorm95(input[0], input[1])
    }
}