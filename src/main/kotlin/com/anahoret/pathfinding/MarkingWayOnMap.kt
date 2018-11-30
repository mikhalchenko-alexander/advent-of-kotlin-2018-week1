package com.anahoret.pathfinding

import java.lang.IllegalArgumentException
import java.util.*

fun addPath(map: String): String {
    return Graph(map).getMapWithPath()
}

class Node(val row: Int,
           val col: Int,
           char: Char) {

    val isWall = char == 'B'
    val isPath = !isWall
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

class Graph(private val map: String) {
    private val graphData = Graph.readGraph(map)
    private val nodes = graphData.nodes
    private val arcs = graphData.arcs
    private val start = nodes.find { it.isStart } ?: throw IllegalArgumentException("No start point specified")
    private val end = nodes.find { it.isEnd } ?: throw IllegalArgumentException("No end point specified")

    private fun calcDistances(): Map<Node, List<Node>> {
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

    fun getMapWithPath(): String {
        val paths = calcDistances()
        return map.lines()
            .map { it.toCharArray() }
            .let { array ->
                array[start.row][start.col] = '*'
                paths.getValue(end).forEach { n ->
                    array[n.row][n.col] = '*'
                }
                array
            }.joinToString(separator = "\n") { it.joinToString(separator = "") }
    }

    companion object {
        private const val STRAIGHT_LENGTH = 2
        private const val DIAGONAL_LENGTH = 3

        private fun List<Node>?.notWallOrNull(idx: Int): Node? = this?.getOrNull(idx)?.takeUnless(Node::isWall)

        private fun readGraph(map: String): GraphData {
            val nodeGrid = map.lines()
                .mapIndexed { row, str -> str.mapIndexed { col, char -> Node(row, col, char) } }

            val arcs = mutableMapOf<Node, List<Arc>>().withDefault { emptyList() }

            nodeGrid.map { nodesRow ->
                nodesRow.map { node ->
                    val maybeRow = nodeGrid.getOrNull(node.row)
                    val maybeTopRow = nodeGrid.getOrNull(node.row - 1)
                    val maybeBottomRow = nodeGrid.getOrNull(node.row + 1)
                    val nodeArcs = listOfNotNull(
                        maybeTopRow.notWallOrNull(node.col)?.let { Arc(it, STRAIGHT_LENGTH) },
                        maybeTopRow.notWallOrNull(node.col - 1)?.let { Arc(it, DIAGONAL_LENGTH) },
                        maybeTopRow.notWallOrNull(node.col + 1)?.let { Arc(it, DIAGONAL_LENGTH) },

                        maybeBottomRow.notWallOrNull(node.col)?.let { Arc(it, STRAIGHT_LENGTH) },
                        maybeBottomRow.notWallOrNull(node.col - 1)?.let { Arc(it, DIAGONAL_LENGTH) },
                        maybeBottomRow.notWallOrNull(node.col + 1)?.let { Arc(it, DIAGONAL_LENGTH) },

                        maybeRow.notWallOrNull(node.col - 1)?.let { Arc(it, STRAIGHT_LENGTH) },
                        maybeRow.notWallOrNull(node.col + 1)?.let { Arc(it, STRAIGHT_LENGTH) }
                    )
                    arcs.put(node, nodeArcs)
                }
            }

            return GraphData(nodeGrid.flatten().filter(Node::isPath), arcs)
        }
    }

    private class GraphData(val nodes: List<Node>, val arcs: Map<Node, List<Arc>>)

}
