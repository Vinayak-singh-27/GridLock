package com.beats.model;

public class Ambulance extends Vehicle {
    public Ambulance() {
        super(VehicleType.AMBULANCE, 5.5, 19.4, 10); // ~70 km/h, highest priority
    }
    @Override public String getDisplayColor() { return "#F44336"; }
    @Override public boolean isEmergency()    { return true; }
}
