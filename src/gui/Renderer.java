package gui;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import paths.CrossroadType;
import paths.ICrossroad;
import paths.IPath;
import structures.Area;
import structures.IRange;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;

public class Renderer implements IRenderer {

    private static final int CROSSROAD_SIZE = 8;
    private static final int HIGHLIGHTED_CROSSROAD_SIZE = 20;
    private static final int PADDING = CROSSROAD_SIZE * 5;
    private static final int FONT_SIZE = 14;
    private static final int MIN_AREA_SIZE = 30;

    private Canvas canvas;
    private GraphicsContext context;
    private boolean withLabels = true;
    private boolean withGrid = true;
    private boolean withLegend = true;

    private Consumer<IRange<Point2D, Double>> handleSelectArea;
    private Point2D start;
    private Point2D end;

    private double rangeX;
    private double rangeY;
    private double sizeX;
    private double sizeY;
    private double scaleX = 1;
    private double scaleY = 1;
    private double minX = 0;
    private double minY = 0;

    public Renderer(Canvas canvas, Consumer<IRange<Point2D, Double>> handleSelectArea) {
        this.canvas = canvas;
        this.handleSelectArea = handleSelectArea;
        context = canvas.getGraphicsContext2D();
        context.setTextAlign(TextAlignment.CENTER);
        context.setTextBaseline(VPos.CENTER);
        resize();
        setupAreaSelection();
    }

    private void renderHighlightedCrossroad(ICrossroad crossroad) {
        context.setFill(Color.RED);

        context.fillOval(
                normX(crossroad.getCoords().getX()) - HIGHLIGHTED_CROSSROAD_SIZE / 2,
                normY(crossroad.getCoords().getY()) - HIGHLIGHTED_CROSSROAD_SIZE / 2,
                HIGHLIGHTED_CROSSROAD_SIZE,
                HIGHLIGHTED_CROSSROAD_SIZE
        );
    }

    @Override
    public void render(ICrossroad[] crossroads, IPath[] paths, List<ICrossroad> highlightedCrossroads, IPath[] highlightedPaths, IRange<Point2D, Double> area) {
        clear();

        if (area != null) {
            renderArea(area);
        }

        renderGrid();

        for (IPath path : paths) {
            renderPath(path, path.isEnabled() ? PathType.BASIC : PathType.DISABLED);
        }

        if (highlightedPaths != null) {
            for (IPath path : highlightedPaths) {
                renderPath(path, PathType.HIGHLIGHTED);
            }
        }

        if (highlightedCrossroads != null) {
            for (ICrossroad crossroad : highlightedCrossroads) {
                renderHighlightedCrossroad(crossroad);
            }
        }

        for (ICrossroad crossroad : crossroads) {
            renderCrossroad(crossroad);
        }

        renderLegend();
    }

    private void renderArea(IRange<Point2D, Double> area) {
        context.setFill(Color.rgb(240, 240, 255));
        context.setStroke(Color.BLUE);
        context.setLineWidth(2);

        double x1 = normX(area.getFrom().getX());
        double y1 = normY(area.getFrom().getY());
        double x2 = normX(area.getTo().getX());
        double y2 = normY(area.getTo().getY());

        context.fillRect(x1, y1, x2 - x1, y2 - y1);
        context.strokeRect(x1, y1, x2 - x1, y2 - y1);

        context.setFill(Color.BLUE);
        context.setTextAlign(TextAlignment.RIGHT);
        context.setTextBaseline(VPos.BOTTOM);
        context.fillText("[" + ((int) area.getFrom().getX()) + ", " + ((int) area.getFrom().getY()) + "]", x1, y1);
        context.setTextAlign(TextAlignment.LEFT);
        context.setTextBaseline(VPos.TOP);
        context.fillText("[" + ((int) area.getTo().getX()) + ", " + ((int) area.getTo().getY()) + "]", x2, y2);
        context.setTextAlign(TextAlignment.CENTER);
        context.setTextBaseline(VPos.CENTER);
    }

    @Override
    public void resize() {
        sizeX = canvas.getWidth();
        sizeY = canvas.getHeight();
        updateScale();
    }

    @Override
    public void rescale(ICrossroad[] crossroads) {
        minX = Double.MAX_VALUE;
        minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (ICrossroad crossroad : crossroads) {
            if (crossroad.getCoords().getX() > maxX) {
                maxX = crossroad.getCoords().getX();
            }

            if (crossroad.getCoords().getX() < minX) {
                minX = crossroad.getCoords().getX();
            }

            if (crossroad.getCoords().getY() > maxY) {
                maxY = crossroad.getCoords().getY();
            }

            if (crossroad.getCoords().getY() < minY) {
                minY = crossroad.getCoords().getY();
            }
        }

        if (minX == maxX) {
            minX -= 0.5;
            maxX += 0.5;
        }

        if (minY == maxY) {
            minY -= 0.5;
            maxY += 0.5;
        }

        rangeX = maxX - minX;
        rangeY = maxY - minY;

        updateScale();
    }

    private void updateScale() {
        double width = sizeX - PADDING * 2;
        double height = sizeY - PADDING * 2;

        scaleX = Math.max(1, (rangeY / rangeX) * (width / height));
        scaleY = Math.max(1, (rangeX / rangeY) * (height / width));
    }

    private void clear() {
        context.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void renderPoint(double x, double y, CrossroadType type) {
        context.setFill((type == CrossroadType.BASIC ? Color.BLACK : (type == CrossroadType.STATION ? Color.BLUE : Color.GREEN))); // TODO: Color from Enum or Map?

        context.fillOval(
                x - CROSSROAD_SIZE / 2,
                y - CROSSROAD_SIZE / 2,
                CROSSROAD_SIZE,
                CROSSROAD_SIZE
        );
    }

    private void renderLine(double x1, double y1, double x2, double y2, PathType type) {
        context.setStroke(type.getColor());
        context.setLineWidth(type.getWidth());
        context.setLineDashes(type.getDash(), type.getDash());
        context.strokeLine(x1, y1, x2, y2);
    }

    private void renderCrossroad(ICrossroad crossroad) {
        double x = normX(crossroad.getCoords().getX());
        double y = normY(crossroad.getCoords().getY());

        CrossroadType type = crossroad.getType();
        renderPoint(x, y, type);

        if (withLabels) {
            boolean isTop = y < FONT_SIZE * 3;

            context.fillText(
                    crossroad.getId(),
                    x,
                    y + (isTop ? FONT_SIZE + CROSSROAD_SIZE / 2 : -CROSSROAD_SIZE / 2 - 2 * FONT_SIZE)
            );

            context.fillText(
                    "[" + ((int) crossroad.getCoords().getX()) + ", " + ((int) crossroad.getCoords().getY()) + "]",
                    x,
                    y + (isTop ? 2 * FONT_SIZE + CROSSROAD_SIZE / 2 : -CROSSROAD_SIZE / 2 - FONT_SIZE)
            );
        }
    }

    private void renderPath(IPath path, PathType type) {
        renderLine(
                normX(path.getFrom().getCoords().getX()),
                normY(path.getFrom().getCoords().getY()),
                normX(path.getTo().getCoords().getX()),
                normY(path.getTo().getCoords().getY()),
                type
        );
    }

    /**
     * Get normalized X coordinate.
     */
    private double normX(double coord) {
        return norm(coord, minX,rangeX * scaleX, sizeX, scaleX);
    }

    /**
     * Get normalized Y coordinate.
     * @param coord
     * @return
     */
    private double normY(double coord) {
        return norm(coord, minY,rangeY * scaleY, sizeY, scaleY);
    }

    /**
     * Get normalized coordinate.
     * Recalculate coordinate by ratio of maxX/minX/maxY/minY coordinates and canvas size.
     * Also add padding, so points on edge of canvas will not be cut off.
     * @return Normalized coordinate.
     */
    private double norm(double coord, double min, double range, double size, double scale) {
        double sizeWithoutPadding = size - PADDING * 2;
        double relativePos = (coord - min) / range;
        double pos = relativePos * sizeWithoutPadding;
        double posWithCentering = pos + (scale > 1 ? (size - (size / scale)) / 2 : 0);

        return posWithCentering + PADDING;
    }

    /**
     * Get denormalized X coordinate.
     */
    private double denormX(double coord) {
        return denorm(coord, minX, rangeX * scaleX, sizeX, scaleX);
    }

    /**
     * Get denormalized Y coordinate.
     */
    private double denormY(double coord) {
        return denorm(coord, minY, rangeY * scaleY, sizeY, scaleY);
    }

    /**
     * Get denormalized coordinate (from canvas coordinate to physical coordinate).
     */
    private double denorm(double coord, double min, double range, double size, double scale) {
        double sizeWithoutPadding = size - PADDING * 2;
        return (((coord - PADDING) - ((scale > 1 ? (size - (size / scale)) / 2 : 0))) / sizeWithoutPadding) * range + min;
    }

    /**
     * Add canvas event handlers.
     */
    private void setupAreaSelection() {
        canvas.setOnMousePressed(event -> {
            start = new Point2D.Double(event.getX(), event.getY());
        });

        canvas.setOnMouseDragged(event -> {
            end = new Point.Double(event.getX(), event.getY());

            if (end.distance(start) >= MIN_AREA_SIZE) {
                handleSelectArea.accept(new Area(new Point.Double(denormX(start.getX()), denormY(start.getY())), new Point.Double(denormX(end.getX()), denormY(end.getY()))));
            } else {
                handleSelectArea.accept(null);
            }
        });

        canvas.setOnMouseReleased(event -> {
            handleSelectArea.accept(null);
        });
    }

    @Override
    public void setWithLabels(boolean withLabels) {
        this.withLabels = withLabels;
    }

    @Override
    public void setWithGrid(boolean withGrid) {
        this.withGrid = withGrid;
    }

    @Override
    public void setWithLegend(boolean withLegend) {
        this.withLegend = withLegend;
    }

    /**
     * Render grid to canvas.
     */
    private void renderGrid() {
        if (!withGrid || minX == Double.MAX_VALUE) {
            return;
        }

        context.setStroke(Color.LIGHTGRAY);
        context.setLineWidth(1);
        context.setFill(Color.GRAY);
        context.setTextAlign(TextAlignment.LEFT);

        int linesDistance = 50;
        int stepsX = (int) Math.round(sizeX / linesDistance);

        double fromX = minX - (rangeX * (scaleX - 1) / 2);
        double fromY = minY - (rangeY * (scaleY - 1) / 2);

        int stepsY = (int) Math.round(sizeY / linesDistance);

        for (int i = 0; i <= stepsX; i++) {
            int pos = (int) Math.round((scaleX * rangeX / stepsX) * i + fromX);
            double normPos = normX(pos);
            context.strokeLine(normPos, 0, normPos, sizeY);
            context.fillText(Integer.toString(pos), normPos + 5, 15);
        }

        for (int i = 0; i <= stepsY; i++) {
            int pos = (int) Math.round((scaleY * rangeY / stepsY) * i + fromY);
            double normPos = normY(pos);
            context.strokeLine(0, normPos, sizeX, normPos);
            context.fillText(Integer.toString(pos), 5, normPos - 10);
        }

        context.setTextAlign(TextAlignment.CENTER);
    }

    /**
     * Render legend to canvas.
     */
    private void renderLegend() {
        if (!withLegend) {
            return;
        }

        context.setGlobalAlpha(0.7);
        context.setFill(Color.LIGHTGRAY);
        context.setTextAlign(TextAlignment.LEFT);
        context.fillRect(sizeX - 150, 30, sizeX - 10, 170);

        int startY = 50;
        int startX = (int) Math.round(sizeX - 130);
        int lineHeight = 25;

        context.setGlobalAlpha(1);

        renderPoint(startX, startY, CrossroadType.BASIC);
        context.fillText("Křižovatka", startX + 30, startY);

        renderPoint(startX, startY + lineHeight, CrossroadType.LANDING);
        context.fillText("Odpočívadlo", startX + 30, startY + lineHeight);

        renderPoint(startX, startY + lineHeight * 2, CrossroadType.STATION);
        context.fillText("Zastávka", startX + 30, startY + lineHeight * 2);

        context.setFill(Color.BLACK);

        renderLine(startX, startY + lineHeight * 3, startX + 20, startY + lineHeight * 3, PathType.BASIC);
        context.fillText("Cesta", startX + 30, startY + lineHeight * 3);

        renderLine(startX, startY + lineHeight * 4, startX + 20, startY + lineHeight * 4, PathType.DISABLED);
        context.fillText("Nefunkční", startX + 30, startY + lineHeight * 4);

        renderLine(startX, startY + lineHeight * 5, startX + 20, startY + lineHeight * 5, PathType.HIGHLIGHTED);
        context.fillText("Vyznačená", startX + 30, startY + lineHeight * 5);

        context.setTextAlign(TextAlignment.CENTER);

    }
}
