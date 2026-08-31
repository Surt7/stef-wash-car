package fr.stefwashcar.enums;

public enum AppointmentStatus {

    pending("En attente"),
    confirmed("Confirmé"),
    cancelled("Annulé"),
    no_show("Absent");

    private final String label;

    AppointmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}