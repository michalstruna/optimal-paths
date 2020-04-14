package structures;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Undirected graph with custom node and edge data.
 * @param <TNodeId> Unique identifier for node.
 * @param <TNode> Custom node data.
 * @param <TEdge> Custom edge data.
 */
public interface IGraph<TNodeId, TNode, TEdge> {

    /**
     * Add node to graph.
     * @param nodeId Unique ID of node.
     * @param data Custom data of node.
     * @throws IllegalArgumentException Specified nodeId already exists.
     */
    void addNode(TNodeId nodeId, TNode data) throws IllegalArgumentException;

    /**
     * Get data of node.
     * @param nodeId Unique ID of node.
     * @return Data of node.
     */
    TNode getNode(TNodeId nodeId);

    /**
     * Remove node from graph.
     * @param nodeId Unique ID of node.
     * @throws IllegalArgumentException Specified nodeId was not found.
     */
    void removeNode(TNodeId nodeId) throws IllegalArgumentException;

    /**
     * @return List of nodes.
     */
    List<TNode> getNodes();

    /**
     * Add edge to graph.
     * @param fromId Unique ID of second node.
     * @param toId Unique ID of first node.
     * @param data Data of edge.
     * @throws IllegalArgumentException Edge between these node already exists.
     * @throws NoSuchElementException Specified nodeId was not found.
     */
    void addEdge(TNodeId fromId, TNodeId toId, TEdge data) throws IllegalArgumentException, NoSuchElementException;

    /**
     * Get data od edge.
     * @param fromId Unique ID of start node.
     * @param toId Unique ID of end node.
     * @return Data of edge.
     * @throws NoSuchElementException Specified nodeId was not found.
     */
    TEdge getEdge(TNodeId fromId, TNodeId toId) throws NoSuchElementException;

    /**
     * Remove edge from graph.
     * @param fromId Unique ID of start node.
     * @param toId Unique ID of end node.
     * @throws NoSuchElementException Specified nodeId or edge between these nodes was not found.
     */
    void removeEdge(TNodeId fromId, TNodeId toId) throws NoSuchElementException;

    /**
     * @return List of all edges in graph-
     */
    List<TEdge> getEdges();

    /**
     * Delete all nodes and edges in graph.
     */
    void clear();

    /**
     * Get descendants of node with specified ID.
     * @return List of descendants.
     * @throws NoSuchElementException Specified nodeId was not found.
     */
    List<TNode> getDescendants(TNodeId nodeId) throws NoSuchElementException;

}