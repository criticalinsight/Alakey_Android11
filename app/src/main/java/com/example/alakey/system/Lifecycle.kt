package com.example.alakey.system

/**
 * Stuart Sierra's Component System.
 * Represents a part of the system that has a lifecycle.
 */
interface Component {
    fun start()
    fun stop()
}

/**
 * A System is a composite Component.
 */
class System(private val components: List<Component>) : Component {
    override fun start() {
        components.forEach { it.start() }
    }

    override fun stop() {
        // Stop in reverse order
        components.reversed().forEach { it.stop() }
    }
}
