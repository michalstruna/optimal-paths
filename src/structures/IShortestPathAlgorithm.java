package structures;

import java.util.NoSuchElementException;

/**
 * Algorithm for finding shortest path between two nodes in graph.
 * @param <TNodeId> Type of node ID.
 * @param <TNode> Type of node.
 * @param <TEdge> Type of edge.
 * @param <TSize> Type of edge size.
 */
public interface IShortestPathAlgorithm<TNodeId, TNode, TEdge, TSize> {

    /**
     * Find shortest path in graph between two nodes.
     * @param graph
     * @param fromId ID of first node.
     * @param toId ID of second node.
     * @return Shortest path.
     * @throws NoSuchElementException There is no path between specified nodes or specified nodeId was not found.
     */
    IGraphPath<TNode, TEdge, TSize> findShortestPath(IGraph<TNodeId, TNode, TEdge>  graph, TNodeId fromId, TNodeId toId) throws IllegalArgumentException, NoSuchElementException;

}