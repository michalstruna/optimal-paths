package structures;

/**
 * Range structure. It can be used e. g. for 1D interval (IRange<Double, Void>) or 2D area (IRange<Point2D, Number>) or 3D space (IRange<Point3D, Number>), ...
 * @param <TValue> Type of value. Value of area is Point, value of interval is Number, etc.
 * @param <TComponent> Component type. Interval has no component (number is a plain value), but area has component Number (Point is composed from numbers).
 */
public interface IRange<TValue, TComponent> {

    /**
     * @return Start value.
     */
    TValue getFrom();

    /**
     * @return End value.
     */
    TValue getTo();

    /**
     * Get relation of range depends on value.
     * Example: If you have 2D area, you can test if point is in area.
     * @param value
     * @return CONTAINS if whole value is in range, OVERLAPS if part of value is in range, otherwise NONE.
     */
    RangeRelation getRelation(TValue value);

    /**
     * Get relation of range depends on components.
     * Example: If you have 2D area, you can test if interval is in area in 1st or 2nd dimension.
     * @return CONTAINS if whole value is in range, OVERLAPS if part of value is in range, otherwise NONE.
     */
    RangeRelation getRelation(int axis, TComponent... components);

}