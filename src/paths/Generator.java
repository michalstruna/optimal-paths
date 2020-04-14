package paths;

import structures.IGraph;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class Generator implements IGenerator {

    private static final int CROSSROADS_MIN_DISTANCE = 10;

    private IGraph<String, ICrossroad, IPath> graph;

    public Generator(IGraph<String, ICrossroad, IPath> graph) {
        this.graph = graph;
    }

    @Override
    public void generate(int crossroads, int landings, int stations, int edgeFrequency, double broken, double mapRatio) {
        graph.clear();
        generateCrossroad(crossroads, CrossroadType.BASIC, mapRatio);
        generateCrossroad(landings, CrossroadType.LANDING, mapRatio);
        generateCrossroad(stations, CrossroadType.STATION, mapRatio);
        List<ICrossroad> generatedCrossroads = graph.getNodes();

        for (ICrossroad crossroad : generatedCrossroads) {
            for (int j = 0; j < edgeFrequency; j++) {
                ICrossroad nearest = getNearest(generatedCrossroads, crossroad);
                if (nearest != null) {
                    graph.addEdge(crossroad.getId(), nearest.getId(), new Path(crossroad, nearest, Math.random() > broken));
                }
            }
        }
    }

    /**
     * Generate crossroads of specified type. If map is full, set map bigger.
     * @param count Count of crossroads.
     * @param type Type of crossroads.
     * @param ratio Ratio of map.
     */
    private void generateCrossroad(int count, CrossroadType type, double ratio) {
        int i = 0;
        int j = 0;
        int mapSize = 100;

        while (i < count) {
            ICrossroad crossroad = new Crossroad(Character.toString(type.toString().charAt(0)) + i, new Point(getRandomCoordinate(mapSize, ratio), getRandomCoordinate(mapSize, 1)), type);
            graph.addNode(crossroad.getId(), crossroad);
            i++;

            ICrossroad nearest = getNearest(graph.getNodes(), crossroad);
            double distance = nearest == null ? Double.MAX_VALUE : nearest.getCoords().distance(crossroad.getCoords());

            if (distance < CROSSROADS_MIN_DISTANCE) {
                i--;
                graph.removeNode(crossroad.getId());
                j++;

                if (j > 10) { // When map is full of crossroads, enlarge size of map.
                    mapSize += CROSSROADS_MIN_DISTANCE;
                    j = 0;
                }
            } else {
                j = 0;
            }

        }
    }

    /**
     * Get random coordinate depends on mapSize and map ratio.
     * @param mapSize Map size.
     * @param ratio Map ratio (e. g. 150x50 is 3.0).
     * @return Random coordinate.
     */
    private int getRandomCoordinate(int mapSize, double ratio) {
        return (int) Math.round(Math.random() * mapSize * ratio);
    }

    /**
     * Get nearest crossroad from list which have no direct path with "crossroad".
     */
    private ICrossroad getNearest(List<ICrossroad> crossroads, ICrossroad crossroad) {
        List<ICrossroad> filtered = crossroads.stream().filter(c -> c.getId() != crossroad.getId()).collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return null;
        }

        return filtered.stream().min((c1, c2) -> {
            double distance1 = c1.getCoords().distance(crossroad.getCoords());
            double distance2 = c2.getCoords().distance(crossroad.getCoords());

            IPath path1 = graph.getEdge(crossroad.getId(), c1.getId());
            IPath path2 = graph.getEdge(crossroad.getId(), c2.getId());

            if (path1 != null && path2 != null) {
                return 0;
            } else if (path1 != null) {
                return 1;
            } else if (path2 != null) {
                return -1;
            }

            if (distance1 < distance2) {
                return -1;
            } else {
                return 1;
            }
        }).get();
    }

}
