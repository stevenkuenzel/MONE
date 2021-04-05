package experiments.heightmaps

import elements.phenotype.neuralnetworks.NetworkPhenotype
import experiments.Experiment
import experiments.Reference
import experiments.SampleVector
import util.io.PathUtil
import util.random.RandomProvider
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.abs

class HeightmapGenerator(
    id: Int
) : Experiment(
    id, 24, 1,
    false,
    true, true
) {

    //(2*offset+1)^2-1
    val offset = 2

    override val name: String
        get() = "HMG"
    override val evaluateEachSolutionOnce: Boolean
        get() = true

    override fun evaluate(networkPhenotype: NetworkPhenotype, referenceID: Int) {
        val size = 500
        val rnd = RandomProvider.create()
        val map = Map(size, Array(size * size) { rnd.nextDouble() })

        val img = BufferedImage(size, size, 1)

        for (x in 0 until size) {
            for (y in 0 until size) {
                val value = networkPhenotype.update(createInput(x, y, map, offset))[0]
                map.data[getIndex(x, y, size)] = value


                val r = (value * 255.0).toInt()
                val color = Color(r, r, r)

                img.setRGB(x, y, color.rgb)
            }
        }

        ImageIO.write(
            img,
            "png",
            File(PathUtil.outputDir + "Map_" + networkPhenotype.id + "_${UUID.randomUUID().toString()}.png")
        )
    }

    fun ev2(networkPhenotype: NetworkPhenotype, map: Map, iteration: Int, print: Boolean): Map {
        val rnd = RandomProvider.create()
        val size = map.size
        val img = BufferedImage(size, size, 1)

        val res = Map(map.size, Array(map.data.size) { 0.0 })

        for (x in 0 until size) {
            for (y in 0 until size) {
                val value = networkPhenotype.update(createInput(x, y, map, offset))[0]
                res.data[getIndex(x, y, size)] = value


                if (print) {
                    val r = (value * 255.0).toInt()
                    val color = Color(r, r, r)
                    img.setRGB(x, y, color.rgb)
                }
            }
        }

        if (print) ImageIO.write(
            img,
            "png",
            File(PathUtil.outputDir + "Map_" + networkPhenotype.id + "_$iteration.png")
        )

        return res
    }

    fun createInput(x: Int, y: Int, map: Map, offset: Int = 1): Array<Double> {
        val result = mutableListOf<Double>()

        for (x_ in -offset..offset) {
            for (y_ in -offset..offset) {
                if (x_ == x && y_ == y) continue

                val perturb = random.nextDouble() * 0.2 + 0.9

                result.add(perturb * map.getValueAt(getIndex(x + x_, y + y_, map.size)))

            }
        }

        return result.toTypedArray()
    }

    override fun sampleAgainst(phenotype: NetworkPhenotype, reference: Reference): SampleVector {

        val map = (reference as MapReference).map
        val newMap = Map(map.size, Array(map.size * map.size) { i -> map.getValueAt(i) })

        val size = map.size

//        val img = BufferedImage(size, size, 1)

        for (x in 0 until size) {
            for (y in 0 until size) {

                val value = phenotype.update(createInput(x, y, map, offset))[0]
                newMap.data[getIndex(x, y, size)] = value


//                val r = (value * 255.0).toInt()
//                val color = Color(r, r, r)
//
//                img.setRGB(x, y, color.rgb)
            }
        }

//        ImageIO.write(img, "png", File(PathUtil.outputDir + "Map_" + phenotype.id + ".png"))

        return SampleVector(phenotype.id, reference.id, Array(1) { map.diff(newMap) })
    }


    fun getIndex(x: Int, y: Int, size: Int): Int {
        if (x < 0 || y < 0 || x >= size || y >= size) return -1

        return x * size + y
    }

    init {
        val img = ImageIO.read(File(PathUtil.inputDir + "HMS.png"))!!
        val size = img.width
        val arr = Array(size * size) { 0.0 }

        for (x in 0 until size) {
            for (y in 0 until size) {
                val rgb = img.getRGB(x, y)
                val clr = Color(rgb)
                arr[getIndex(x, y, size)] = clr.red.toDouble() / 255.0
            }
        }

        references.add(MapReference(0, "X", Map(size, arr)))
    }

    class Map(val size: Int, val data: Array<Double>) {
        fun getValueAt(index: Int): Double {
            if (index == -1) return -1.0

            return data[index]
        }

        fun diff(other: Map): Double {
            if (size != other.size) return Double.MAX_VALUE

            var sum = 0.0

            for (index in data.indices) {
                sum += abs(data[index] - other.data[index])
            }

            return sum / (size * size).toDouble()
        }
    }

    class MapReference(id: Int, name: String, val map: Map) : Reference(id, name) {

    }
}