package structures;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class Graph<TNodeId, TNode, TEdge> implements IGraph<TNodeId, TNode, TEdge>, Serializable {

    Map<TNodeId, Node> nodes;

    public Graph() {
        nodes = new HashMap<>();
    }

    @Override
    public void addNode(TNodeId nodeId, TNode nodeData) throws IllegalArgumentException {
        if (nodes.containsKey(nodeId)) {
            throw new IllegalArgumentException("Vrchol " + nodeId + " již existuje.");
        } else {
            Node node = new Node(nodeId, nodeData);
            nodes.put(nodeId, node);
        }
    }

    @Override
    public TNode getNode(TNodeId nodeId) {
        Node node = nodes.get(nodeId);
        return node == null ? null : node.data;
    }

    @Override
    public void removeNode(TNodeId nodeId) throws NoSuchElementException {
        Node node = getRequiredNode(nodeId);
        List<Edge> edges = new ArrayList<>(node.edges);

        for (Edge edge : edges) {
            removeEdge(edge.fromId, edge.toId);
        }

        nodes.remove(nodeId);
    }

    @Override
    public List<TNode> getNodes() {
        return nodes.values().stream().map(node -> node.data).collect(Collectors.toList());
    }

    @Override
    public void addEdge(TNodeId fromId, TNodeId toId, TEdge data) throws IllegalArgumentException {
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Smyčky nejsou povoleny.");
        }

        if (getEdge(fromId, toId) == null) {
            Edge edge = new Edge(fromId, toId, data);
            nodes.get(fromId).edges.add(edge);
            nodes.get(toId).edges.add(edge);
        } else {
            throw new IllegalArgumentException("Hrana mezi vrcholy " + fromId + " a " + toId + " již existuje.");
        }
    }

    @Override
    public TEdge getEdge(TNodeId fromId, TNodeId toId) throws NoSuchElementException {
        Node from = getRequiredNode(fromId);
        Node to = getRequiredNode(toId);
        List<Edge> edges = from.edges.stream().filter(item -> item.getTarget(fromId).equals(toId)).collect(Collectors.toList());

        return edges.isEmpty() ? null : edges.get(0).data;
    }

    @Override
    public void removeEdge(TNodeId fromId, TNodeId toId) throws NoSuchElementException {
        Node from = getRequiredNode(fromId);
        Node to = getRequiredNode(toId);
        Edge edge = from.edges.stream().filter(item -> item.getTarget(fromId).equals(toId)).findFirst().get();

        from.edges.remove(edge);
        to.edges.remove(edge);
    }

    @Override
    public List<TEdge> getEdges() {
        Set<Edge> edges = new HashSet<>();

        for (Node node : nodes.values()) {
            edges.addAll(node.edges);
        }

        return edges.stream().map(edge -> edge.data).collect(Collectors.toList());
    }

    @Override
    public void clear() {
        nodes.clear();
    }

    @Override
    public List<TNode> getDescendants(TNodeId nodeId) throws NoSuchElementException {
        return getRequiredNode(nodeId).edges.stream().map(edge -> {
            TNodeId descendantId = edge.getTarget(nodeId);
            return getNode(descendantId);
        }).collect(Collectors.toList());
    }

    /**
     * Get node structure with specified ID.
     * @param nodeId Unique ID of node.
     * @return Node structure (not node data).
     * @throws IllegalArgumentException Node with specified ID was not found.
     */
    private Node getRequiredNode(TNodeId nodeId) throws NoSuchElementException {
        Node node = nodes.get(nodeId);

        if (node == null) {
            throw new NoSuchElementException("Vrchol " + nodeId + " nebyl nalezen.");
        }

        return node;
    }

    private class Node implements Serializable {

        public TNodeId id;
        public TNode data;
        public List<Edge> edges;

        public Node(TNodeId id, TNode data) {
            this.id = id;
            this.data = data;
            edges = new ArrayList<>();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return node.id.equals(id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

    }

    private class Edge implements Serializable {

        public TNodeId fromId;
        public TNodeId toId;
        public TEdge data;

        public Edge(TNodeId fromId, TNodeId toId, TEdge data) {
            this.fromId = fromId;
            this.toId = toId;
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Edge edge = (Edge) o;
            return fromId.equals(edge.toId) && toId.equals(edge.fromId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fromId, toId) + Objects.hash(toId, fromId);
        }

        public TNodeId getTarget(TNodeId source) {
            return source == fromId ? toId : fromId;
        }
    }

}
