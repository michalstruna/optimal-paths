package structures;

import java.awt.geom.Point2D;
import java.util.List;

public interface IRange2DTree<TNode> {

    /**
     * Find node by coordinates.
     * @param position
     * @return Node on coordinates or null.
     */
    TNode find(Point2D position);

    /**
     * Find nodes by area.
     * @param area
     * @return List of all nodes in area or empty list.
     */
    List<TNode> find(IRange<Point2D, Double> area);

}