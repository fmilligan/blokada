package tunnel

import core.Kontext
import core.Result
import core.Time
import org.pcap4j.packet.Packet
import java.net.DatagramSocket
import java.util.*

internal class Forwarder(val ttl: Time = 10 * 1000) : Iterable<ForwardRule> {

    private val store = LinkedList<ForwardRule>()

    fun add(ktx: Kontext, socket: DatagramSocket, originEnvelope: Packet) {
        if (store.size >= 1024) {
            ktx.v("removing socket because list is full", store.element().socket)
            Result.of { store.element().socket.close() }
            store.remove()
        }
        while (store.isNotEmpty() && store.element().isOld()) {
            ktx.v("removing socket because it is too old", store.element().socket)
            Result.of { store.element().socket.close() }
            store.remove()
        }
        ktx.v("adding socket to list", socket)
        store.add(ForwardRule(socket, originEnvelope, ttl))
    }

    override fun iterator() = store.iterator()

    fun size() = store.size
}

internal data class ForwardRule(
        val socket: DatagramSocket,
        val originEnvelope: Packet,
        val ttl: Time
) {
    val added = System.currentTimeMillis()

    fun isOld(): Boolean {
        return (System.currentTimeMillis() - added) > ttl
    }
}
