package experiments.fightingice.annbot

import de.stevenkuenzel.xml.XAttribute
import de.stevenkuenzel.xml.XElement
import de.stevenkuenzel.xml.XLoadable
import de.stevenkuenzel.xml.XSavable
import enumerate.Action
import ftginterface.StateAction
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Joint probability distribution.
 *
 * @property action The action (skill).
 * @property intervalSize The grid size (N = 20 in my thesis).
 * @property size The number of (successful) samples.
 * @constructor Create empty Joint probability distribution
 */
class JointProbabilityDistribution(val action: Action, val intervalSize: Int, val size: Int) : XSavable {

    /**
     * Constructur for creating a new JPD based on raw data.
     */
    constructor(data: List<StateAction>, intervalSize: Int) : this(data.first().action, intervalSize, data.size) {
        create(data)
        initialize()
    }

    companion object : XLoadable<JointProbabilityDistribution> {
        override fun fromXElement(xElement: XElement, vararg optional: Any): JointProbabilityDistribution {

            val intervalSize = xElement.getAttributeValueAsInt("Interval")
            val size = xElement.getAttributeValueAsInt("Count")
            val action = Action.valueOf(xElement.getAttributeValueAsString("Name"))

            val jpd = JointProbabilityDistribution(action, intervalSize, size)

            val xIndices = xElement.getChild("Indices")!!

            val strIX = xIndices.getAttributeValueAsString("X")
            jpd.vX = strIX.split(",").map { it.toInt() }.toMutableList()
            val strIY = xIndices.getAttributeValueAsString("Y")
            jpd.vY = strIY.split(",").map { it.toInt() }.toMutableList()

            val xValues = xElement.getChild("Values")!!

            val strPX = xValues.getAttributeValueAsString("PX")
            jpd.pX = strPX.split(",").map { it.toInt() }.toTypedArray()
            val strPY = xValues.getAttributeValueAsString("PY")
            jpd.pY = strPY.split(",").map { it.toInt() }.toTypedArray()
            val strPXY = xValues.getAttributeValueAsString("PXY")
            jpd.pXY = strPXY.split(",").map { it.toInt() }.toTypedArray()

            jpd.initialize()

            return jpd
        }
    }


    // Lists of considered indices / nodes.
    var vX = mutableListOf<Int>()
    var vY = mutableListOf<Int>()

    // Range counters (number of successful attacks per range).
    lateinit var pXY: Array<Int>
    lateinit var pX: Array<Int>
    lateinit var pY: Array<Int>

    /**
     * Maximum occurring probability.
     */
    var maximumProbability = 0.0

    /**
     * 95 % percentile of probabilities.
     */
    var percentile95Probability = 0.0

    /**
     * Creates the JPD based on the successful samples provided in _data_.
     *
     * @param data All successful samples.
     */
    private fun create(data: List<StateAction>) {
        assert(data.all { it.success })
        assert(data.all { it.action == action })

        // Find all nodes to consider.
        for (d in data) {
            val dX = roundNextN(d.distanceX)
            val dY = roundNextN(d.distanceY)

            if (!vX.contains(dX)) vX.add(dX)
            if (!vY.contains(dY)) vY.add(dY)
        }

        vX.sortBy { it }
        vY.sortBy { it }

        // Count the number of successful attacks.
        pXY = Array(vY.size * vX.size) { 0 }
        pX = Array(vX.size) { 0 }
        pY = Array(vY.size) { 0 }

        for (d in data) {
            val dX = roundNextN(d.distanceX)
            val dY = roundNextN(d.distanceY)

            val iX = vX.indexOf(dX)
            val iY = vY.indexOf(dY)

            pXY[iY * vX.size + iX] += 1

            pX[iX] += 1
            pY[iY] += 1
        }

// Only for debugging:
//        var strDXY = ""
//        var strDX = ""
//        var strDY = ""
//        val indicesX = Array(30) { i -> (i + 1) * 10 }
//        val indicesY = Array(60) { i -> (i - 29) * 10 }
//
//        for (x in indicesX) {
//            val iX = vX.indexOf(x)
//            val p = if (iX == -1) 0.0 else pX[iX].toDouble() / size.toDouble()
//
//            strDX += "(${x}, ${p}) "
//        }
//
//        for (y in indicesY) {
//            val iY = vY.indexOf(y)
//            val p = if (iY == -1) 0.0 else pY[iY].toDouble() / size.toDouble()
//
//            strDY += "(${y}, ${p}) "
//        }
//
//
//        for (x in indicesX) {
//            for (y in indicesY) {
//                val p = getJointProbability(x, y)
//                strDXY += "($x, $y, $p)"
//            }
//        }
//
//        println(action.name + ":")
//        println("X: " + strDX)
//        println("Y: " + strDY)
//        println("XY: " + strDXY)
//        println()
    }

    /**
     * Determines the max. probability and 95 % percentile.
     *
     */
    private fun initialize() {
        val ds = DescriptiveStatistics()

        for (x in vX) {
            for (y in vY) {
                val p = getJointProbability(x, y)

                ds.addValue(p)

                if (p > maximumProbability) maximumProbability = p
            }
        }

        percentile95Probability = ds.getPercentile(95.0)
    }

    /**
     * Returns the next node value.
     *
     * @param value The input to round to the next node.
     * @return The next node.
     */
    private fun roundNextN(value: Int): Int {
        if (value == 0) return intervalSize
        if (value % intervalSize == 0) return value

        return (ceil(value.toDouble() / intervalSize.toDouble()) * (if (value < 0) -1.0 else 1.0) * intervalSize.toDouble()).toInt()
    }

    /**
     * Returns the value in relation to the 95 % percentile for an (x, y)-distance vector.
     */
    fun getJointProbabilityNorm95(x_: Int, y_: Int): Double {
        val p = getJointProbability(x_, y_) / percentile95Probability

        return p.coerceIn(0.0, 1.0)
    }

    /**
     * Returns the value for an (x, y)-distance vector.
     */
    fun getJointProbability(x_: Int, y_: Int): Double {
        val x = roundNextN(x_)
        val y = roundNextN(y_)

        val iX = vX.indexOf(x)
        if (iX == -1) return 0.0

        val iY = vY.indexOf(y)
        if (iY == -1) return 0.0

        return pXY[iY * vX.size + iX].toDouble() / size.toDouble()
    }

    override fun toXElement(): XElement {
        val strIndicesX = vX.toString().replace(" ", "")
        val strIndicesY = vY.toString().replace(" ", "")
        val strPXY = pXY.contentToString().replace(" ", "")
        val strPX = pX.contentToString().replace(" ", "")
        val strPY = pY.contentToString().replace(" ", "")

        val xElement = XElement(
            "Action",
            XAttribute("Name", action.name),
            XAttribute("Count", size),
            XAttribute("Interval", intervalSize)
        )
        val xIndices = xElement.addChild("Indices")
        xIndices.addAttribute("X", strIndicesX.substring(1, strIndicesX.length - 1))
        xIndices.addAttribute("Y", strIndicesY.substring(1, strIndicesY.length - 1))

        val xValues = xElement.addChild("Values")
        xValues.addAttribute("PXY", strPXY.substring(1, strPXY.length - 1))
        xValues.addAttribute("PX", strPX.substring(1, strPX.length - 1))
        xValues.addAttribute("PY", strPY.substring(1, strPY.length - 1))

        return xElement
    }
}