package paths;

import java.awt.geom.Point2D;
import java.io.Serializable;

public class Crossroad implements ICrossroad, Serializable {

    private String id;
    private CrossroadType type;
    private Point2D coords;

    public Crossroad(String id, Point2D coords, CrossroadType type) {
        this.id = id;
        this.type = type;
        this.coords = coords;
    }

    public Crossroad(String id, Point2D coords) {
        this(id, coords, CrossroadType.BASIC);
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setType(CrossroadType type) {
        this.type = type;
    }

    @Override
    public CrossroadType getType() {
        return type;
    }

    @Override
    public void setCoords(Point2D coords) {
        this.coords = coords;
    }

    @Override
    public Point2D getCoords() {
        return coords;
    }

    @Override
    public String toString() {
        return getType().toString() + " " + getId() + " [" +  ((int) (getCoords().getX())) + ", " + ((int) (getCoords().getY())) + "]";
    }

}
