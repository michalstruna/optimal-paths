package paths;

public interface IGenerator {

    /**
     * Generate random map (crossroads and paths).
     * @param crossroads Count of plain crossroads.
     * @param landing Count of landings.
     * @param stations Count of stations.
     * @param edgeFrequency Count of paths generated for each crossroad.
     * @param broken Relative amount of paths that will be disabled.
     * @param mapRatio Should be ratio of current canvas width and height, so generated map can fill whole screen.
     */
    void generate(int crossroads, int landing, int stations, int edgeFrequency, double broken, double mapRatio);

}
