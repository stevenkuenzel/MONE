package experiments.dpb

import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import kotlin.math.cos
import kotlin.math.sin

/**
 * An AWT Component for visualizing the Double Pole Balancing experiment. Based on the implementation provided in ANJI.
 *
 * SOURCE: http://anji.sourceforge.net/
 */
class DPBVisualization(val trackLength: Double, val poleLengths: Array<Double>) : Component() {

    private val DISPLAY_CART_WIDTH = 20
    private val DISPLAY_CART_HEIGHT = 5

    private val trackLengthHalf = trackLength / 2.0

    private var cartPos = 0.0
    private var poleAngles = Array(poleLengths.size) { 0.0 }


    init {
        background = Color.WHITE
    }

    /**
     * Paint the cart, poles and track.
     *
     * @param g The Graphics instance.
     */
    override fun paint(g: Graphics) {
        val orig = g.color
        val displayTrackLength = (width * 0.80).toInt()
        val scaleRatio = displayTrackLength / trackLength

        // Track.
        g.color = Color.BLACK
        val displayTrackYPos = (height * 0.90).toInt()
        val displayTrackLeftXPos = width / 2 - displayTrackLength / 2
        g.drawLine(
            displayTrackLeftXPos, displayTrackYPos,
            displayTrackLeftXPos + displayTrackLength, displayTrackYPos
        )

        // Cart.
        g.color = Color.GREEN
        val displayCartCenterXPos = (displayTrackLeftXPos
                + (displayTrackLength * ((cartPos + trackLengthHalf) / trackLength)).toInt())
        val displayCartLeftXPos = (displayCartCenterXPos - DISPLAY_CART_WIDTH.toDouble() / 2).toInt()
        g.fillRect(
            displayCartLeftXPos, displayTrackYPos - DISPLAY_CART_HEIGHT, DISPLAY_CART_WIDTH,
            DISPLAY_CART_HEIGHT
        )

        // Poles.
        val colors = mutableListOf<Color>()
        colors.add(Color.BLUE)
        colors.add(Color.RED)

        for (i in poleAngles.indices) {
            g.color = colors[i]

            val displayPoleLength = poleLengths[i] * scaleRatio
            val radians = poleAngles[i] * Math.PI

            val x = sin(radians) * displayPoleLength
            val y = cos(radians) * displayPoleLength
            g.drawLine(
                displayCartCenterXPos, displayTrackYPos - DISPLAY_CART_HEIGHT,
                (displayCartCenterXPos + x).toInt(),
                (displayTrackYPos - DISPLAY_CART_HEIGHT - y).toInt()
            )
        }
        g.color = orig
    }

    /**
     * Update the information base on the experiment's state.
     */
    fun step(cartPosition: Double, poleAngles: Array<Double>) {

        require(poleLengths.size == poleAngles.size) {
            ("wrong # poles, expected " + poleLengths.size
                    + ", got " + poleAngles.size)
        }

        cartPos = when {
            cartPosition < -trackLengthHalf -> {
                -trackLengthHalf
            }
            cartPosition > trackLengthHalf -> {
                trackLengthHalf
            }
            else -> {
                cartPosition
            }
        }

        this.poleAngles = poleAngles
    }
}