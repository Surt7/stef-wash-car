package fr.stefwashcar.enums;

public enum Weekday {

    Monday(1, "Lundi"),
    Tuesday(2, "Mardi"),
    Wednesday(3, "Mercredi"),
    Thursday(4, "Jeudi"),
    Friday(5, "Vendredi"),
    Saturday(6, "Samedi"),
    Sunday(7, "Dimanche");

    private final int value;
    private final String label;

    Weekday(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static Weekday fromValue(int value) {
        for (Weekday weekday : values()) {
            if (weekday.value == value) {
                return weekday;
            }
        }

        throw new IllegalArgumentException("Invalid weekday value: " + value);
    }
}