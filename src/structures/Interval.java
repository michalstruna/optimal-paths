package structures;

public class Interval implements IRange<Double, Void> {

    private Double from;
    private Double to;

    public Interval(Double from, Double to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public Double getFrom() {
        return from;
    }

    @Override
    public Double getTo() {
        return to;
    }

    @Override
    public RangeRelation getRelation(Double value) {
        return from <= value && to >= value ? RangeRelation.CONTAINS : RangeRelation.NONE;
    }

    @Override
    public RangeRelation getRelation(int axis, Void... voids) {
        return RangeRelation.NONE;
    }

}
