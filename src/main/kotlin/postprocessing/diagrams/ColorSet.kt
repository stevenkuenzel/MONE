package postprocessing.diagrams

import org.apache.commons.math3.util.FastMath
import java.awt.Color

/**
 * Contains a color and some variations of that color.
 *
 * @property base The color.
 */
class ColorSet(val base : Color) {
    private val factor = 0.75

    val shade = computeShade()
    val tint = computeTint()
    val contrast = computeContrastColor()

    private fun computeShade() : Color {
        val r_ = base.red
        val g_ = base.green
        val b_ = base.blue

        val r = FastMath.round(r_ * factor).toInt()
        val g = FastMath.round(g_ * factor).toInt()
        val b = FastMath.round(b_ * factor).toInt()

        return Color(r, g, b)
    }

    private fun computeTint() : Color  {
        val r_ = base.red
        val g_ = base.green
        val b_ = base.blue

        val r = (r_ + FastMath.round((255 - r_) * factor).toInt()) / 255f
        val g = (g_ + FastMath.round((255 - g_) * factor).toInt()) / 255f
        val b = (b_ + FastMath.round((255 - b_) * factor).toInt()) / 255f

        return Color(r, g, b, 0.5f)
    }

    /**
     * SOURCE: https://stackoverflow.com/questions/4672271/reverse-opposing-colors
     */
    private fun computeContrastColor() : Color {
        val y = (299 * base.red + 587 * base.green + 114 * base.blue) / 1000.0
        return if (y >= 128) Color.black else Color.white
    }
}