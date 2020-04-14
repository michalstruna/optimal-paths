package paths;

import structures.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Forest implements IForest {

    private IGraph<String, ICrossroad, IPath> graph;
    private IRange2DTree<ICrossroad> tree;
    private Runnable handleChange;

    public Forest(Runnable handleChange) {
        graph = new Graph<>();
        tree = new Range2DTree<>(Arrays.asList(getCrossroads()), crossroad -> crossroad.getCoords());

        this.handleChange = () -> {
            tree = new Range2DTree<>(Arrays.asList(getCrossroads()), crossroad -> crossroad.getCoords());
            handleChange.run();
        };
    }

    @Override
    public void addCrossroad(ICrossroad crossroad) throws IllegalArgumentException {
        if (getCrossroad(crossroad.getCoords()) != null) {
            throw new IllegalArgumentException("Křižovatka na pozici [" + crossroad.getCoords().getX() + ", " + crossroad.getCoords().getY() + "] již existuje.");
        }

        graph.addNode(crossroad.getId(), crossroad);
        handleChange.run();
    }

    @Override
    public ICrossroad getCrossroad(String crossroadId) {
        return graph.getNode(crossroadId);
    }

    @Override
    public ICrossroad getCrossroad(Point2D coords) {
        return tree.find(coords);
    }

    @Override
    public void removeCrossroad(String crossroadId) throws IllegalArgumentException {
        graph.removeNode(crossroadId);
        handleChange.run();
    }

    @Override
    public void updateCrossroad(String crossroadId, ICrossroad updated) throws NoSuchElementException, IllegalArgumentException {
        ICrossroad current = graph.getNode(crossroadId);
        ICrossroad crossroadOnPosition = getCrossroad(updated.getCoords());

        if (current == null) {
            throw new NoSuchElementException("There is no node with ID " + crossroadId + ".");
        } else if (crossroadOnPosition != null && crossroadOnPosition.getId().equals(updated.getId())) {
                throw new IllegalArgumentException("Křižovatka na pozici [" + updated.getCoords().getX() + ", " + updated.getCoords().getY() + "] již existuje.");
        } else {
            current.setCoords(updated.getCoords());
            current.setType(updated.getType());

            if (!current.getId().equals(updated.getId())) {
                List<ICrossroad> descendants = graph.getDescendants(current.getId());
                List<IPath> paths = descendants.stream().map(descendant -> {
                    IPath path = graph.getEdge(descendant.getId(), current.getId());

                    if (path.getFrom().getId().equals(current.getId())) {
                        path.setFrom(updated);
                    } else {
                        path.setTo(updated);
                    }

                    return path;
                }).collect(Collectors.toList());
                graph.removeNode(crossroadId);
                graph.addNode(updated.getId(), updated);

                for (IPath path : paths) {
                    graph.addEdge(updated.getId(), path.getFrom().getId().equals(updated.getId()) ? path.getTo().getId() : path.getFrom().getId(), path);
                }

            }

            handleChange.run();
        }
    }

    @Override
    public ICrossroad[] getCrossroads() {
        return graph.getNodes().toArray(new ICrossroad[0]);
    }

    @Override
    public ICrossroad[] getCrossroads(CrossroadType type) {
        return graph.getNodes().stream().filter(node -> node.getType().equals(type)).toArray(ICrossroad[]::new);
    }

    @Override
    public ICrossroad[] getCrossroads(IRange<Point2D, Double> area) {
        return tree.find(area).toArray(new ICrossroad[0]);
    }

    @Override
    public void addPath(IPath path) throws IllegalArgumentException {
        graph.addEdge(path.getFrom().getId(), path.getTo().getId(), path);
        handleChange.run();
    }

    @Override
    public IPath getPath(String fromId, String toId) throws IllegalArgumentException {
        return graph.getEdge(fromId, toId);
    }

    @Override
    public void removePath(String fromId, String toId) throws IllegalArgumentException {
        graph.removeEdge(fromId, toId);
        handleChange.run();
    }

    @Override
    public void updatePath(String fromId, String toId, IPath updated) throws NoSuchElementException, IllegalArgumentException {
        IPath path = graph.getEdge(fromId, toId);

        if (path == null) {
            throw new NoSuchElementException("Cesta mezi " + fromId + " a " + toId + " neexistuje.");
        } else {
            try {
                removePath(fromId, toId);
                addPath(updated);
            } catch (IllegalArgumentException exception) {
                addPath(path);
                throw exception;
            }

            handleChange.run();
        }
    }

    @Override
    public IPath[] getPaths() {
        return graph.getEdges().toArray(new IPath[0]);
    }

    @Override
    public IPath[] getPaths(ICrossroad crossroad) {
        List<ICrossroad> descendants = graph.getDescendants(crossroad.getId());
        Set<IPath> paths = new HashSet<>();

        for (ICrossroad descendant : descendants) {
            paths.add(graph.getEdge(crossroad.getId(), descendant.getId()));
        }

        return paths.toArray(new IPath[0]);
    }

    @Override
    public IPath[] getPaths(boolean isEnabled) {
        return graph.getEdges().stream().filter(edge -> edge.isEnabled() == isEnabled).toArray(IPath[]::new);
    }

    @Override
    public IGraphPath<ICrossroad, IPath, Double> findShortestPath(String fromId, String toId) throws NoSuchElementException {
        IShortestPathAlgorithm<String, ICrossroad, IPath, Double> algorithm = new Dijkstra<>(node -> node.getId(), edge -> edge.getSize(), (x, y) -> x + y, Double::compare, edge -> edge.isEnabled());
        return algorithm.findShortestPath(graph, fromId, toId);
    }

    @Override
    public IRoutingMatrix<ICrossroad> getRoutingMatrix() {
        Function<ICrossroad, String> idAccessor = node -> node.getId();
        IShortestPathAlgorithm<String, ICrossroad, IPath, Double> dijkstra = new Dijkstra<>(idAccessor, edge -> edge.getSize(), (x, y) -> x + y, Double::compare);
        IRoutingMatrixCalculator<String, ICrossroad, IPath> calculator = new RoutingMatrixCalculator(dijkstra, idAccessor);
        return calculator.getRoutingMatrix(graph);
    }

    @Override
    public void generate(int crossroads, int landings, int stations, int pathsFrequency, double broken, double mapRatio) {
        IGenerator generator = new Generator(graph);
        generator.generate(crossroads, landings, stations, pathsFrequency, broken, mapRatio);
        handleChange.run();
    }

    @Override
    public void clear() {
        graph.clear();
        handleChange.run();
    }

    @Override
    public void save(String fileName) throws IOException {
        FileOutputStream fs = new FileOutputStream(fileName);
        ObjectOutputStream os = new ObjectOutputStream(fs);
        os.writeObject(graph);
        os.close();
        fs.close();
    }

    @Override
    public void load(String fileName) throws IOException, ClassNotFoundException {
        FileInputStream fs = new FileInputStream(fileName);
        ObjectInputStream os = new ObjectInputStream(fs);
        graph = (IGraph) os.readObject();
        handleChange.run();
    }
}
