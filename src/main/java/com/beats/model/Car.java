package com.beats.model;

public class Car extends Vehicle {
    public Car() {
        super(VehicleType.CAR, 4.5, 13.9, 1); // ~50 km/h
    }
    @Override public String getDisplayColor() { return "#2196F3"; }
    @Override public boolean isEmergency()    { return false; }
}
