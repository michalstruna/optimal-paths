package gui;

import paths.ICrossroad;
import paths.IPath;
import structures.IRange;

import java.awt.geom.Point2D;
import java.util.List;


public interface IRenderer {

    /**
     * Re-render all objects on canvas.
     */
    void render(ICrossroad[] crossroads, IPath[] paths, List<ICrossroad> highlightedCrossroads, IPath[] highlightedPaths, IRange<Point2D, Double> area);

    /**
     * Resize canvas depends on windows size.
     */
    void resize();

    /**
     * Rescale canvas depends on min/max x/y coords of crossroads.
     * @param crossroads Array of all rendered crossroads.
     */
    void rescale(ICrossroad[] crossroads);

    /**
     * Set visibility of labels.
     */
    void setWithLabels(boolean withLabels);

    /**
     * Set visibility of grid.
     */
    void setWithGrid(boolean withGrid);

    /**
     * Set visibility of legend.
     */
    void setWithLegend(boolean withLegend);

}
