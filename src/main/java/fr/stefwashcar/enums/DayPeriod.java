package fr.stefwashcar.enums;

public enum DayPeriod {

    AM("Matin"),
    PM("Après-midi");

    private final String label;

    DayPeriod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}