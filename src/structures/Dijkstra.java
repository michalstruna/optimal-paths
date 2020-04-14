package structures;

import javafx.util.Pair;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Dijkstra<TNodeId, TNode, TEdge, TSize> implements IShortestPathAlgorithm<TNodeId, TNode, TEdge, TSize> {

    private Function<TNode, TNodeId> idAccessor;
    private Function<TEdge, TSize> sizeAccessor;
    private Function<TEdge, Boolean> isEnabledAccessor;
    private IGraph<TNodeId, TNode, TEdge> graph;
    private Map<TNodeId, TSize> distancesFromStart;
    private BiFunction<TSize, TSize, TSize> sizeCounter;
    private Comparator<TSize> sizeComparator;
    private TNodeId fromId;

    public Dijkstra(
            Function<TNode, TNodeId> idAccessor,
            Function<TEdge, TSize> sizeAccessor,
            BiFunction<TSize, TSize, TSize> sizeCounter,
            Comparator<TSize> sizeComparator
    )  {
        this(idAccessor, sizeAccessor, sizeCounter, sizeComparator, edge -> true);
    }

    public Dijkstra(
            Function<TNode, TNodeId> idAccessor,
            Function<TEdge, TSize> sizeAccessor,
            BiFunction<TSize, TSize, TSize> sizeCounter,
            Comparator<TSize> sizeComparator,
            Function<TEdge, Boolean> isEnabledAccessor
    ) {
        this.idAccessor = idAccessor;
        this.sizeAccessor = sizeAccessor;
        this.isEnabledAccessor = isEnabledAccessor;
        this.sizeCounter = sizeCounter;
        this.sizeComparator = Comparator.nullsLast(sizeComparator);
    }

    /**
     * TODO: Refactor, split to more methods.
     * Find shortest path in graph between node with fromId and node with toId using Dijkstra algorithm.
     * First, set distances of all nodes from start node to Infinity and add start node to queue.
     * Repeat this algorithm until end node is reached:
     *     - Get node from queue with highest priority,
     *     - Loop all descendants of this node,
     *     - If descendant is end node, build path from start node to this descendant and exit,
     *     - If descendant was not already visited, add it to queue.
     * @param graph
     * @param fromId ID of first node.
     * @param toId ID of second node.
     * @return Shortest path between nodes.
     * @throws IllegalArgumentException
     * @throws NoSuchElementException
     */
    @Override
    public IGraphPath<TNode, TEdge, TSize> findShortestPath(IGraph<TNodeId, TNode, TEdge> graph, TNodeId fromId, TNodeId toId) throws IllegalArgumentException, NoSuchElementException {
        this.fromId = fromId;
        this.graph = graph;
        distancesFromStart = getInitDistances();
        PriorityQueue<TNode> queue = new PriorityQueue<>(this::comparePriority);
        queue.add(graph.getNode(fromId));

        Set<TNode> visited = new HashSet<>();
        Map<TNodeId, Pair<TNode, TEdge>> predecessors = new HashMap<>();
        TNode current;

        while (!queue.isEmpty()) {
            current = queue.poll();
            visited.add(current);

            TNodeId currentId = idAccessor.apply(current);
            TSize currentDistance = distancesFromStart.get(currentId);
            List<TNode> descendants = graph.getDescendants(currentId);

            for (TNode descendant : descendants) {
                TNodeId descendantId = idAccessor.apply(descendant);
                TEdge edge = graph.getEdge(currentId, descendantId);

                if (!isEnabledAccessor.apply(edge) || visited.contains(descendant)) {
                    continue;
                }

                TSize edgeCost = sizeAccessor.apply(edge);
                TSize totalDistance = currentDistance == null ? edgeCost : sizeCounter.apply(currentDistance, edgeCost);

                if (descendantId.equals(toId)) {
                    distancesFromStart.put(descendantId, totalDistance);
                    predecessors.put(descendantId, new Pair<>(current, edge));
                    return buildPath(descendant, predecessors);
                }

                if (sizeComparator.compare(totalDistance, distancesFromStart.get(descendantId)) < 0) {
                    distancesFromStart.put(descendantId, totalDistance);
                    queue.remove(descendant);
                    queue.add(descendant);
                    predecessors.put(descendantId, new Pair<>(current, edge));
                }
            }
        }

        throw new NoSuchElementException("Cesta mezi vrcholy " + fromId + " a " + toId + " nebyla nalezena.");
    }

    /**
     * Build path with "node" and all its predecessors.
     * @param node End node.
     * @param predecessors Map where key is nodeId and value predecessor. Predecessor is pair contains node and its edge.
     * @return Path.
     */
    IGraphPath<TNode, TEdge, TSize> buildPath(TNode node, Map<TNodeId, Pair<TNode, TEdge>> predecessors) {
        List<TEdge> edges = new ArrayList<>();
        List<TNode> nodes = new ArrayList<>();
        TNode current = node;
        Pair<TNode, TEdge> predecessor;

        while ((predecessor = predecessors.get(idAccessor.apply(current))) != null) {
            edges.add(predecessor.getValue());
            nodes.add(predecessor.getKey());
            current = predecessor.getKey();
        }

        Collections.reverse(edges);
        Collections.reverse(nodes);
        nodes.add(node);

        return new GraphPath<>(nodes, edges, distancesFromStart.get(idAccessor.apply(node)));
    }

    /**
     * @return Map where keys are ID of node and values are distance from start (initialize to null).
     */
    private Map<TNodeId, TSize> getInitDistances() {
        Map<TNodeId, TSize> distances = new HashMap<>();

        for (TNode node : graph.getNodes()) {
            TNodeId nodeId = idAccessor.apply(node);
            distances.put(nodeId, null);
        }

        return distances;
    }

    /**
     * Compare priority. Start node has highest priority, then sizeComparator is used.
     */
    private int comparePriority(TNode node1, TNode node2) {
        TNodeId node1Id = idAccessor.apply(node1);
        TNodeId node2Id = idAccessor.apply(node2);

        if (node1Id.equals(fromId)) {
            return -1;
        } else if (node2Id.equals(fromId)) {
            return 1;
        }

        TSize distance1 = distancesFromStart.get(node1Id);
        TSize distance2 = distancesFromStart.get(node2Id);

        return sizeComparator.compare(distance1, distance2);
    }

}