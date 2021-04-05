package util.io

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Util that provides the necessary path references.
 */
class PathUtil {
    companion object
    {
        /**
         * Working dir of the application.
         */
        val workingDir = Paths.get("").toAbsolutePath().toString() + "/"

        /**
         * Output dir, where data is saved to.
         */
        val outputDir = "${workingDir}output/"

        /**
         * Input dir, where data is loaded from.
         */
        val inputDir = "${workingDir}input/"

        init {
            // Create the directories, if not existing.

            if (!Files.exists(Paths.get(outputDir)))
            {
                Files.createDirectory(Paths.get(outputDir))
            }
            else
            {
                // Clear the content of the output dir.
                File(outputDir).listFiles().forEach { it.deleteRecursively() }
            }

            if (!Files.exists(Paths.get(inputDir)))
            {
                Files.createDirectory(Paths.get(inputDir))
            }
        }

        /**
         * Returns the absolute path to a file (starting at the working dir).
         *
         */
        fun getAbsolutePath(file : String) : String
        {
            if (Paths.get(file).isAbsolute)
            {
                return file
            }

            return workingDir + file
        }
    }
}