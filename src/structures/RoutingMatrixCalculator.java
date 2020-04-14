package structures;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class RoutingMatrixCalculator<TNodeId, TNode, TEdge> implements IRoutingMatrixCalculator<TNodeId, TNode, TEdge> {

    private IShortestPathAlgorithm shortestPathAlgorithm;
    Function<TNode, TNodeId> idAccessor;

    public RoutingMatrixCalculator(IShortestPathAlgorithm shortestPathAlgorithm, Function<TNode, TNodeId> idAccessor) {
        this.shortestPathAlgorithm = shortestPathAlgorithm;
        this.idAccessor = idAccessor;
    }

    /**
     * Loop nodes in 2 nested for cycles (find shortest path between each two nodes).
     * However "processPath" method is used, so there is no need to calculate path between each two nodes,
     * if these nodes is on path which was calculated earlier.
     */
    @Override
    public RoutingMatrix<TNode> getRoutingMatrix(IGraph<TNodeId, TNode, TEdge> graph) throws NoSuchElementException {
        List<TNode> nodes = graph.getNodes();

        if (nodes.isEmpty()) {
            throw new NoSuchElementException("Nelze vypočítat směrovací matici z prázdného grafu.");
        }

        TNode[][] matrix = getInitialMatrix(graph);

        if (nodes.isEmpty()) {
            return new RoutingMatrix<>(getNodesArrayFromList(new ArrayList<>()), matrix);
        }

        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 0; j < nodes.size(); j++) {
                if (i == j || matrix[i][j] != null) { // If i and j are same node or if there is already calculated value.
                    continue;
                }

                TNodeId fromId = idAccessor.apply(nodes.get(i));
                TNodeId toId = idAccessor.apply(nodes.get(j));

                try {
                    IGraphPath<TNode, TEdge, ?> shortestPath = shortestPathAlgorithm.findShortestPath(graph, fromId, toId);
                    processPath(shortestPath, matrix, nodes);
                } catch (NoSuchElementException exception) {
                    // There is no path between i and j nodes.
                }
            }
        }

        return new RoutingMatrix<>(getNodesArrayFromList(nodes), matrix);
    }

    /**
     * There is no need to calc shortest path from each node to each node.
     * Shortest path between two nodes gets also shortest paths between all nodes on this path.
     * Example: Shortest path is v1 -> v2 -> v3 -> v4. Then shortest paths are also v1 -> v2, v2 -> v3, v2 -> v4, v4 -> v2, v3 -> v2, etc.
     */
    private void processPath(IGraphPath<TNode, TEdge, ?> path, TNode[][] matrix, List<TNode> nodes) {
        List<TNode> pathNodes = path.getNodes();

        for (int k = 1; k < pathNodes.size(); k++) {
            for (int l = 0; l < pathNodes.size() - k; l++) {
                TNode crossroad = pathNodes.get(l);
                TNode target = pathNodes.get(l + k);
                TNode next = pathNodes.get(l + 1);
                TNode before = pathNodes.get(l + k - 1);

                int indexFrom = nodes.indexOf(crossroad);
                int indexTarget = nodes.indexOf(target);

                matrix[indexFrom][indexTarget] = next;
                matrix[indexTarget][indexFrom] = before;
            }
        }
    }

    /**
     * For O(1) access convert list to arrays.
     * There must be @SupressWarnings("unchecked") and reflection, because Java cannot create generic array from list.
     * @param nodes List of nodes.
     * @return Array of nodes.
     */
    private TNode[] getNodesArrayFromList(List<TNode> nodes) {
        TNode nodeId = nodes.get(0);

        @SuppressWarnings("unchecked")
        TNode[] result = (TNode[]) Array.newInstance(nodeId.getClass(), nodes.size());
        nodes.toArray(result);
        return result;
    }

    /**
     * Create empty matrix of size NxN where N is count of nodes.
     */
    private TNode[][] getInitialMatrix(IGraph graph) {
        List<TNode> nodes = graph.getNodes();
        int matrixDimension = nodes.size();

        if (nodes.isEmpty()) {
            return null;
        }

        TNode node = nodes.get(0);

        @SuppressWarnings("unchecked")
        TNode[][] matrix = (TNode[][]) Array.newInstance(node.getClass(), matrixDimension, matrixDimension);

        return matrix;
    }

}
