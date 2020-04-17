package paths;

import java.awt.geom.Point2D;
import java.io.Serializable;

public interface ICrossroad extends Serializable {

    /**
     * @param id  ew ID of crossroad.
     */
    void setId(String id);

    /**
     * @return ID of crossroad.
     */
    String getId();

    /**
     * @param type New type of crossroad.
     */
    void setType(CrossroadType type);

    /**
     * @return Type of crossroad.
     */
    CrossroadType getType();

    /**
     * @param coords New coords of crossroad.
     */
    void setCoords(Point2D coords);

    /**
     * @return Coords of crossroad.
     */
    Point2D getCoords();

}
