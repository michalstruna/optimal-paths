package paths;

import structures.GraphPath;

import java.util.Arrays;
import java.util.List;

public class ForestPath extends GraphPath<ICrossroad, IPath, Double> {

    public ForestPath(List<ICrossroad> crossroads, List<IPath> paths, double size) {
        super(crossroads, paths, size);
    }

    public ForestPath(IPath path) {
        this(Arrays.asList(path.getFrom(), path.getTo()), Arrays.asList(path), path.getSize());
    }
}