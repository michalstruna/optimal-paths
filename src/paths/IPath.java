package paths;

public interface IPath {

    /**
     * @param from Start node of path.
     */
    void setFrom(ICrossroad from);

    /**
     * @return Start node of path.
     */
    ICrossroad getFrom();

    /**
     * @param from End node of path.
     */
    void setTo(ICrossroad from);

    /**
     * @return End node of path.
     */
    ICrossroad getTo();

    /**
     * @return Size of path.
     */
    double getSize();

    /**
     * @param isEnabled Path is enabled.
     */
    void setEnabled(boolean isEnabled);

    /**
     * @return Path is enabled.
     */
    boolean isEnabled();

}