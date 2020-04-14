package paths;

import java.io.Serializable;

public class Path implements IPath, Serializable {

    private ICrossroad from;
    private ICrossroad to;
    private boolean isEnabled;

    public Path(ICrossroad from, ICrossroad to, boolean isEnabled) {
        this.from = from;
        this.to = to;
        this.isEnabled = isEnabled;
    }

    public Path(ICrossroad from, ICrossroad to) {
        this(from, to, true);
    }

    @Override
    public void setFrom(ICrossroad from) {
        this.from = from;
    }

    @Override
    public ICrossroad getFrom() {
        return from;
    }

    @Override
    public void setTo(ICrossroad to) {
        this.to = to;
    }

    @Override
    public ICrossroad getTo() {
        return to;
    }

    @Override
    public double getSize() {
        return from.getCoords().distance(to.getCoords());
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }
}
