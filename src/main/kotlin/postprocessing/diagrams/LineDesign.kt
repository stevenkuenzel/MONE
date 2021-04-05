package postprocessing.diagrams

import postprocessing.diagrams.ColorSet
import java.awt.BasicStroke
import java.awt.Shape

/**
 * Contains information about the design (color, shape and stroke) of a line in a diagram.
 */
class LineDesign(val colorSet: ColorSet, val shape: Shape, stroke_: FloatArray) {
    val stroke = BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0f, stroke_, 0f)
}