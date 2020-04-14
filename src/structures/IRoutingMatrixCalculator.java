package structures;

import java.util.NoSuchElementException;

public interface IRoutingMatrixCalculator<TNodeId, TNode, TEdge> {

    /**
     * Get routing matrix of graph.
     * @param graph
     * @return Routing matrix.
     * @throws NoSuchElementException Graph is empty.
     */
    IRoutingMatrix<TNode> getRoutingMatrix(IGraph<TNodeId, TNode, TEdge> graph) throws NoSuchElementException;

}