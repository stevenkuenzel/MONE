package postprocessing.diagrams

import org.apache.xmlgraphics.java2d.GraphicContext
import org.apache.fop.svg.PDFDocumentGraphics2D
import org.jfree.chart.JFreeChart
import util.io.PathUtil
import java.awt.Rectangle
import java.io.FileOutputStream

/**
 * Extends the corresponding class of Apache.FOP. Adds further necessary information and function.
 */
class ExtPDFDocumentGraphics2D(val chart: JFreeChart, val file: String, val w: Int, val h: Int) :
    PDFDocumentGraphics2D(false) {

    fun apply() {
        graphicContext = GraphicContext()

        setupDocument(FileOutputStream(PathUtil.outputDir + file + ".pdf"), w, h)
        setClip(0, 0, 0, 0)
        chart.draw(this, Rectangle(w, h))
        finish()
    }
}