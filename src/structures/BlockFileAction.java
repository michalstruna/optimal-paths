package structures;

public enum BlockFileAction {
    FILE_CREATED("Soubor vytvořen"),
    FILE_OPENED("Soubor otevřen"),
    CONTROL_BLOCK_READ("Řídící blok přečten"),
    SEARCH_START("Hledání začalo"),
    READ_BLOCK("Přečten blok"),
    RELATIVE_DISTANCE_CALCULATED("Relativní vzdálenost"),
    RECORD_FOUND("Záznam nalezen"),
    RECORD_NOT_FOUND("Záznam nebyl nalezen"),
    SEARCH_ANOTHER_BLOCK("Přehledat blok"),
    SEARCH_INTERVAL("Prohledat oblast"),

    REMOVE_START("Odebírá se"),
    RECORD_REMOVED("Záznam odebrán"),
    EXCEPTION("Exception");

    private String name;

    BlockFileAction(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
