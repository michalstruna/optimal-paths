package structures;

public interface IRoutingMatrix<TNode> {

    /**
     * @return Array of all nodes.
     */
    TNode[] getNodes();

    /**
     * @return Routing matrix in same order as nodes.
     */
    TNode[][] getRouting();

}
