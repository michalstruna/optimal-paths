package gui;

import javafx.scene.paint.Color;

public enum PathType {

    BASIC(1, Color.BLACK, 0),
    HIGHLIGHTED(5, Color.RED, 0),
    DISABLED(1, Color.RED, 8);

    private int width;
    private Color color;
    private double dash;

    private PathType(int width, Color color, double dash) {
        this.width = width;
        this.color = color;
        this.dash = dash;
    }

    public int getWidth() {
        return width;
    }

    public Color getColor() {
        return color;
    }

    public double getDash() {
        return dash;
    }
}
