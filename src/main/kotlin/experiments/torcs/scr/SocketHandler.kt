package experiments.torcs.scr

import java.io.IOException
import java.lang.Exception
import java.net.*

/**
 * Kotlin translation of the original SCR TORCS implementation.
 *
 * SOURCE: Loiacono, Daniele, Luigi Cardamone, and Pier Luca Lanzi. "Simulated car racing championship: Competition software manual." arXiv preprint arXiv:1304.1672 (2013).
 */
class SocketHandler(host: String, val port: Int, private val verbose: Boolean) {
    val address = InetAddress.getByName(host)
    val socket = DatagramSocket()

    fun send(msg: String) {
        if (verbose) println("Sending: $msg")
        try {
            val buffer = msg.toByteArray()
            socket.send(
                DatagramPacket(buffer, buffer.size, address, port)
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun receive(): String? {
        try {
            val buffer = ByteArray(1024)
            val packet = DatagramPacket(buffer, buffer.size)

            socket.receive(packet)

            val received = String(packet.data, 0, packet.length)

            if (verbose) println("Received: $received")

            return received
        } catch (se: SocketTimeoutException) {
            if (verbose) println("Socket Timeout!")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun receive(timeout: Int): String? {
        try {
            socket.soTimeout = timeout
            val received = receive()
            socket.soTimeout = 0

            return received
        } catch (e: SocketException) {
            e.printStackTrace()
        }

        return null
    }

    fun close() {
        socket.close()
    }
}