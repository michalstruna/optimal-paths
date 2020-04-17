package structures;

public enum BlockFileAction {
    FILE_CREATED("Soubor vytvořen"),
    FILE_OPENED("Soubor otevřen"),

    BLOCK_READ("Přečten blok z disku"),
    BLOCK_WRITTEN("Zapsán blok na disk"),
    CONTROL_BLOCK_READ("Přečten řídící blok z disku"),
    CONTROL_BLOCK_WRITTEN("Zapsán řídící blok na disk"),

    SEARCH_START("Hledání začalo"),
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
