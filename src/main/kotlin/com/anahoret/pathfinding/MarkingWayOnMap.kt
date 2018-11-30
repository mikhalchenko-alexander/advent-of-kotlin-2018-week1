package com.anahoret.pathfinding

import java.lang.IllegalArgumentException
import java.util.*

fun addPath(map: String): String = Graph.getMapWithPath(map)

object Graph {

    private const val STRAIGHT_LENGTH = 2
    private const val DIAGONAL_LENGTH = 3

    fun getMapWithPath(map: String): String {
        val (nodes, arcs) = readGraph(map)
        val start = nodes.find { it.isStart } ?: throw IllegalArgumentException("No start point specified")
        val end = nodes.find { it.isEnd } ?: throw IllegalArgumentException("No end point specified")

        val paths = calcDistances(start, nodes, arcs)
        return map.lines()
            .map(String::toCharArray)
            .let { charGrid ->
                charGrid[start.row][start.col] = '*'
                paths.getValue(end).forEach { charGrid[it.row][it.col] = '*' }
                charGrid.joinToString(separator = "\n") { row -> row.joinToString(separator = "") }
            }
    }

    private fun calcDistances(start: Node, nodes: Collection<Node>, arcs: Map<Node, List<Arc>>): Map<Node, List<Node>> {
        val paths = nodes.map { it to emptyList<Node>() }.toMap().toMutableMap()
        val distances = nodes.map { it to if (it == start) 0 else Int.MAX_VALUE }.toMap().toMutableMap()
        val visited = mutableSetOf<Node>()

        val queue = PriorityQueue<Node>(nodes.size) { n1, n2 -> distances.getValue(n1) - distances.getValue(n2) }
        queue.addAll(nodes)

        while (queue.isNotEmpty()) {
            val node = queue.poll()
            visited.add(node)
            arcs.getValue(node)
                .filterNot { visited.contains(it.node) }
                .forEach { arc ->
                    if (distances.getValue(node) + arc.length < distances.getValue(arc.node)) {
                        distances[arc.node] = distances.getValue(node) + arc.length
                        paths[arc.node] = paths.getValue(node) + arc.node
                        queue.remove(arc.node)
                        queue.add(arc.node)
                    }
                }
        }

        return paths.toMap()
    }

    private fun readGraph(map: String): Pair<List<Node>, Map<Node, List<Arc>>> {
        val nodes = map.lines()
            .mapIndexed { row, str ->
                str.mapIndexedNotNull { col, char -> if (char == 'B') null else Node(row, col, char) }
            }
            .flatten()

        val arcs = nodes.map { node ->
            val row = nodes.filter { it.row == node.row }
            val topRow = nodes.filter { it.row == node.row - 1 }
            val bottomRow = nodes.filter { it.row == node.row + 1 }
            val nodeArcs = listOfNotNull(
                topRow.find { it.col == node.col }?.let { Arc(it, STRAIGHT_LENGTH) },
                topRow.find { it.col == node.col - 1 }?.let { Arc(it, DIAGONAL_LENGTH) },
                topRow.find { it.col == node.col + 1 }?.let { Arc(it, DIAGONAL_LENGTH) },

                bottomRow.find { it.col == node.col }?.let { Arc(it, STRAIGHT_LENGTH) },
                bottomRow.find { it.col == node.col - 1 }?.let { Arc(it, DIAGONAL_LENGTH) },
                bottomRow.find { it.col == node.col + 1 }?.let { Arc(it, DIAGONAL_LENGTH) },

                row.find { it.col == node.col - 1 }?.let { Arc(it, STRAIGHT_LENGTH) },
                row.find { it.col == node.col + 1 }?.let { Arc(it, STRAIGHT_LENGTH) }
            )
            node to nodeArcs
        }.toMap()

        return Pair(nodes, arcs)
    }

    class Node(val row: Int, val col: Int, char: Char) {

        val isStart = char == 'S'
        val isEnd = char == 'X'

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Node) return false

            if (row != other.row) return false
            if (col != other.col) return false

            return true
        }

        override fun hashCode(): Int {
            var result = row
            result = 31 * result + col
            return result
        }

    }

    class Arc(val node: Node, val length: Int)

}
