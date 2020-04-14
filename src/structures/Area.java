package structures;

import java.awt.geom.Point2D;

public class Area implements IRange<Point2D, Double> {

    private Point2D from;
    private Point2D to;

    public Area(Point2D point1, Point2D point2) {
        from = getMin(point1, point2);
        to = getMax(point1, point2);
    }

    @Override
    public Point2D getFrom() {
        return from;
    }

    @Override
    public Point2D getTo() {
        return to;
    }

    @Override
    public RangeRelation getRelation(Point2D point) {
        return point.getX() >= from.getX() && point.getX() <= to.getX() && point.getY() >= from.getY() && point.getY() <= to.getY() ? RangeRelation.CONTAINS : RangeRelation.NONE;
    }

    /**
     * We have area that is created from two points, e. g. [1, 2] and [4, 8].
     * Then X range of area is 1-4 and Y range of area is 2-8.
     * Safe range to min and max variables. If axis == 0, safe X range, if axis == 1, safe Y range.
     * Then check if whole range, only part of range or none from range is from area.
     */
    @Override
    public RangeRelation getRelation(int axis, Double... coords) {
        if (coords.length >= 2 && (axis == 0 || axis == 1)) {
            double min = axis == 0 ? from.getX() : from.getY();
            double max = axis == 0 ? to.getX() : to.getY();

            if (min <= coords[0] && max >= coords[1]) {
                return RangeRelation.CONTAINS;
            } else if (min > coords[1] || max < coords[0]) {
                return RangeRelation.NONE;
            } else {
                return RangeRelation.OVERLAPS;
            }
        }

        return RangeRelation.NONE;
    }

    /**
     * Get most left-top point from points.
     * Example: point1 = [2, 10]; point = [3, 5], then result is [2, 3].
     */
    private Point2D getMin(Point2D point1, Point2D point2) {
        return new Point2D.Double(Math.min(point1.getX(), point2.getX()), Math.min(point1.getY(), point2.getY()));
    }

    /**
     * Get most right-bottom point from points.
     * Example: point1 = [2, 10]; point = [3, 5], then result is [10, 5].
     */
    private Point2D getMax(Point2D point1, Point2D point2) {
        return new Point2D.Double(Math.max(point1.getX(), point2.getX()), Math.max(point1.getY(), point2.getY()));
    }

}
