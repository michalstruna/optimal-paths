package paths;

import structures.IGraphPath;
import structures.IRange;
import structures.IRoutingMatrix;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.NoSuchElementException;

public interface IForest {

    /**
     * Add new crossroad.
     * @param crossroad
     * @throws IllegalArgumentException Node with specified ID or coords already exists.
     */
    void addCrossroad(ICrossroad crossroad) throws IllegalArgumentException;

    /**
     * @param crossroadId
     * @return Crossroad with specified id.
     */
    ICrossroad getCrossroad(String crossroadId);

    /**
     * @param coords
     * @return Crossroad on specified coordinates.
     */
    ICrossroad getCrossroad(Point2D coords);

    /**
     * Remove crossroad with specified ID from paths.
     * @param crossroadId ID of node.
     * @throws NoSuchElementException Node with specified ID was not found.
     */
    void removeCrossroad(String crossroadId) throws NoSuchElementException;

    /**
     * Update crossroad by ID.
     * @param crossroadId ID of crossroad.
     * @param updated Updated crossroad.
     * @throws NoSuchElementException Crossroad with specified ID was not found.
     * @throws IllegalArgumentException Crossroad with updated ID or coords already exists.
     */
    void updateCrossroad(String crossroadId, ICrossroad updated) throws NoSuchElementException, IllegalArgumentException;

    /**
     * Get all crossroads.
     * @return Array of all crossroads.
     */
    ICrossroad[] getCrossroads();

    /**
     * Get crossroads with specified type.
     * @param type Type of crossroad.
     * @return Array of crossroads with type.
     */
    ICrossroad[] getCrossroads(CrossroadType type);

    /**
     * Get crossroads in area.
     * @param area
     * @return Array of crossroads in area.
     */
    ICrossroad[] getCrossroads(IRange<Point2D, Double> area);

    /**
     * Add new path.
     * @param path
     * @throws IllegalArgumentException Path between specified nodes already exists.
     */
    void addPath(IPath path) throws IllegalArgumentException;

    /**
     * Get path between specified nodes.
     * @param fromId ID of start node.
     * @param toId ID of end node.
     * @return Path between specified nodes.
     * @throws NoSuchElementException Node with specified ID was not found.
     */
    IPath getPath(String fromId, String toId) throws NoSuchElementException;

    /**
     * Remove path between specified nodes from paths.
     * @param fromId ID of start node.
     * @param toId ID of end node.
     * @throws NoSuchElementException Edge between specified nodes or nodeId was not found.
     */
    void removePath(String fromId, String toId) throws NoSuchElementException;

    /**
     * Update existing path.
     * @param fromId
     * @param toId
     * @param updated Updated path.
     * @throws NoSuchElementException Crossroad with specified ID or path between specified crossroads was not found.
     * @throws IllegalArgumentException Edge with updated crossroads already exists.
     */
    void updatePath(String fromId, String toId, IPath updated) throws NoSuchElementException, IllegalArgumentException;

    /**
     * Get all paths.
     * @return Array of all paths.
     */
    IPath[] getPaths();

    /**
     * Get all paths from/to crossroad.
     * @param crossroad
     * @return Array of paths.
     */
    IPath[] getPaths(ICrossroad crossroad);

    /**
     * Get all paths that are enabled or disabled.
     * @param isEnabled
     * @return Array of paths that are enabled or disabled.
     */
    IPath[] getPaths(boolean isEnabled);

    /**
     * Find shortest path between two crossroads.
     * @param fromId ID of start crossroad.
     * @param toId ID of end crossroad.
     * @return Shortest path.
     */
    IGraphPath<ICrossroad, IPath, Double> findShortestPath(String fromId, String toId) throws NoSuchElementException;

    /**
     * @return Routing matrix.
     */
    IRoutingMatrix<ICrossroad> getRoutingMatrix();

    /**
     * Generate random map.
     * @param crossroads Count of crossroads.
     * @param landings Count of landings.
     * @param stations Count of stations.
     * @param mapRatio
     * @param pathsFrequency Count of paths for each crossroad.
     * @param broken Relative amount of paths thar will be disabled.
     */
    void generate(int crossroads, int landings, int stations, int pathsFrequency, double broken, double mapRatio);

    /**
     * Remove all crossroads and paths.
     */
    void clear();

    /**
     * Save map to file.
     * @param fileName
     */
    void save(String fileName) throws IOException;

    /**
     * Load map from file.
     * @param fileName
     */
    void load(String fileName) throws IOException, ClassNotFoundException;

}
