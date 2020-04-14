package structures;

import java.util.Arrays;
import java.util.List;

public class GraphPath<TNode, TEdge, TSize> implements IGraphPath<TNode, TEdge, TSize> {

    private List<TNode> nodes;
    private List<TEdge> edges;
    private TSize size;

    public GraphPath(List<TNode> nodes, List<TEdge> edges, TSize size) {
        this.nodes = nodes;
        this.edges = edges;
        this.size = size;
    }

    public GraphPath(List<TNode> nodes, TEdge edge, TSize size) {
        this(nodes, Arrays.asList(edge), size);
    }

    @Override
    public List<TNode> getNodes() {
        return nodes;
    }

    @Override
    public List<TEdge> getEdges() {
        return edges;
    }

    @Override
    public TSize getSize() {
        return size;
    }

}
