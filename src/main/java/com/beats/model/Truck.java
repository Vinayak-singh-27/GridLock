package com.beats.model;

public class Truck extends Vehicle {
    public Truck() {
        super(VehicleType.TRUCK, 16.0, 8.3, 2); // ~30 km/h
    }
    @Override public String getDisplayColor() { return "#795548"; }
    @Override public boolean isEmergency()    { return false; }
}
