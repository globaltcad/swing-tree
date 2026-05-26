package examples.trains.mvi;

/** Whether the station board shows trains leaving or trains coming in. */
public enum BoardMode {
    DEPARTURES("Departures"),
    ARRIVALS("Arrivals");

    private final String label;
    BoardMode(String label) { this.label = label; }
    public String label() { return label; }
    public boolean isArrivals() { return this == ARRIVALS; }
}