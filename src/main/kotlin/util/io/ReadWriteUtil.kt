package util.io

import java.io.File

/**
 * Util that provides reading and writing capability.
 */
class ReadWriteUtil {
    companion object
    {
        fun writeToFile(fileName : String, content : String) : String
        {
            val path = PathUtil.getAbsolutePath("output/$fileName")

            File(path).writeText(content)

            return path
        }

        fun readFromFile(fileName : String) : String
        {
            val path = PathUtil.getAbsolutePath("$fileName")

            return File(path).readText()
        }

        fun readLinesFromFile(fileName : String) : List<String>
        {
            val path = PathUtil.getAbsolutePath("$fileName")

            return File(path).readLines()
        }
    }
}