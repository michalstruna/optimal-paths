package structures;

import java.util.List;

public interface IGraphPath<TNode, TEdge, TSize> {

    /**
     * @return List of all nodes on graph path.
     */
    List<TNode> getNodes();

    /**
     * @return List of all edges on graph path.
     */
    List<TEdge> getEdges();

    /**
     * @return Total cost of all edges on graph path.
     */
    TSize getSize();

}
