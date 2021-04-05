package experiments.fightingice.annbot

import de.stevenkuenzel.xml.XElement
import de.stevenkuenzel.xml.XLoadable
import de.stevenkuenzel.xml.XSavable
import enumerate.Action
import ftginterface.StateAction

/**
 * Loads raw state-action-success information from the XMl-files. Allows the creation of the according JPDs.
 *
 * @constructor Creates a new instance.
 */
class RawInformationLoader private constructor() {
    val actionMap = hashMapOf<Action, List<StateAction>>()

    companion object : XLoadable<RawInformationLoader> {
        override fun fromXElement(xElement: XElement, vararg optional: Any): RawInformationLoader {

            val result = RawInformationLoader()

            for (xAction in xElement.getChildren("Action")) {
                val action = Action.valueOf(xAction.getAttributeValueAsString("Name"))
                val data = mutableListOf<StateAction>()

                for (xStateAction in xAction.getChildren("StateAction")) {
                    val stateAction = StateAction.loadFromXML(xStateAction)
                    data.add(stateAction)
                }

                result.actionMap[action] = data
            }

            return result
        }
    }

    /**
     * Creates the JPDs based on the information provided in the XML-files.
     *
     * @param intervalSize Grid-size (N=20 in my thesis).
     * @return The list of JPDs.
     */
    fun createJPDs(intervalSize: Int) : MutableList<JointProbabilityDistribution>
    {
        val result = mutableListOf<JointProbabilityDistribution>()
        val sortedKeys = actionMap.keys.sorted()

        for (key in sortedKeys) {
            result.add(JointProbabilityDistribution(actionMap[key]!!.filter { it.success }, intervalSize))
        }

        return result
    }
}