package paths;

public enum CrossroadType {

    BASIC("Křižovatka"),
    LANDING("Odpočívadlo"),
    STATION("Zastávka");

    private final String name;

    private CrossroadType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}