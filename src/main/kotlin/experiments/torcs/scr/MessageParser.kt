package experiments.torcs.scr

import java.lang.Exception
import java.util.*

/**
 * Kotlin translation of the original SCR TORCS implementation.
 *
 * SOURCE: Loiacono, Daniele, Luigi Cardamone, and Pier Luca Lanzi. "Simulated car racing championship: Competition software manual." arXiv preprint arXiv:1304.1672 (2013).
 */
class MessageParser(val message : String) {

    // parses the message from the serverbot, and creates a table of
    // associated names and values of the readings
    private val table = Hashtable<String, Any>()

    init {
        val mt = StringTokenizer(message, "(")

        while (mt.hasMoreElements()) {
            // process each reading
            var reading = mt.nextToken()

            val endOfMessage = reading.indexOf(")")
            if (endOfMessage > 0) {
                reading = reading.substring(0, endOfMessage)
            }

            val rt = StringTokenizer(reading, " ")

            if (rt.countTokens() >= 2) {
                val readingName = rt.nextToken()

                var readingValue: Any = ""

                if (readingName == "opponents" || readingName == "track" || readingName == "wheelSpinVel" || readingName == "focus") { //ML
                    // these readings have multiple values
                    readingValue = DoubleArray(rt.countTokens())

                    var position = 0

                    while (rt.hasMoreElements()) {
                        val nextToken = rt.nextToken()

                        try {
                            readingValue[position] = nextToken.toDouble()
                        } catch (e: Exception) {
                            println("Error parsing value '$nextToken' for $readingName using 0.0")
                            println("Message: $message")
                            readingValue[position] = 0.0
                        }

                        position++
                    }
                } else {
                    val token = rt.nextToken()

                    readingValue = try {
                        token.toDouble()
                    } catch (e: Exception) {
                        println("Error parsing value '$token' for $readingName using 0.0")
                        println("Message: $message")
                        0.0
                    }
                }

                table[readingName] = readingValue
            }
        }
    }

    fun printAll() {
        val keys = table.keys()

        while (keys.hasMoreElements()) {
            val key = keys.nextElement()
            print("$key:  ")
            println(table[key])
        }
    }

    fun getReading(key: String?): Any? {
        return table[key]
    }
}