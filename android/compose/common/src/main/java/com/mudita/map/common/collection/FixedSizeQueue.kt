package com.mudita.map.common.collection

class FixedSizeQueue<T>(val maxSize: Int): Collection<T> {
    private val arrayDeque = ArrayDeque<T>(maxSize)

    override val size: Int get() = arrayDeque.size

    fun add(element: T) {
        with(arrayDeque) {
            if (size >= maxSize) {
                removeFirst()
            }
            addLast(element)
        }
    }

    fun remove(element: T) {
        arrayDeque.remove(element)
    }

    override fun contains(element: T): Boolean =
        arrayDeque.contains(element)

    override fun containsAll(elements: Collection<T>): Boolean =
        arrayDeque.containsAll(elements)

    override fun isEmpty(): Boolean = arrayDeque.isEmpty()

    override fun iterator(): Iterator<T> = arrayDeque.iterator()
}

fun FixedSizeQueue<Double>.average(): Double = sumOf { it } / size
