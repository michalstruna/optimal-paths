package structures;

public class RoutingMatrix<TNode> implements IRoutingMatrix<TNode> {

    private TNode[] nodes;
    private TNode[][] routing;

    public RoutingMatrix(TNode[] nodes, TNode[][] routing) {
        this.nodes = nodes;
        this.routing = routing;
    }

    @Override
    public TNode[] getNodes() {
        return nodes;
    }

    @Override
    public TNode[][] getRouting() {
        return routing;
    }
}
