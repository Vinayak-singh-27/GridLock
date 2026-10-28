package com.beats.model;

public class Bus extends Vehicle {
    public Bus() {
        super(VehicleType.BUS, 12.0, 11.1, 2); // ~40 km/h
    }
    @Override public String getDisplayColor() { return "#FF9800"; }
    @Override public boolean isEmergency()    { return false; }
}
