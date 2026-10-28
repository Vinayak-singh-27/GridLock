package com.beats.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single directional lane within a road segment.
 * Contains a detection line (at detectionThreshold) and a stop line (at 1.0).
 */
public class Lane {

    private final String id;
    private final String fromIntersectionId;
    private final String toIntersectionId;
    private final String direction;        // N, S, E, W, NE, NW, SE, SW
    private final double lengthMeters;
    private final double detectionThreshold; // 0.0-1.0, fraction of lane where detection line sits

    private boolean blocked;
    private final List<Vehicle> vehicles;

    public Lane(String id, String fromIntersectionId, String toIntersectionId,
                String direction, double lengthMeters) {
        this.id = id;
        this.fromIntersectionId = fromIntersectionId;
        this.toIntersectionId = toIntersectionId;
        this.direction = direction;
        this.lengthMeters = lengthMeters;
        this.detectionThreshold = 0.70; // detection line at 70% of lane
        this.blocked = false;
        this.vehicles = new ArrayList<>();
    }

    // ---- Detection / queue helpers ----

    /** Vehicles that have crossed the detection line and are waiting */
    public int getDetectedWaitingCount() {
        return (int) vehicles.stream()
                .filter(v -> v.isDetected() && v.isWaitingAtRed())
                .count();
    }

    /** All vehicles that have crossed detection line */
    public int getDetectedCount() {
        return (int) vehicles.stream().filter(Vehicle::isDetected).count();
    }

    /** Vehicles currently in the lane */
    public int getQueueLength() { return vehicles.size(); }

    /** Maximum single-vehicle wait time in ms */
    public long getMaxWaitMs(long nowMs) {
        return vehicles.stream()
                .mapToLong(v -> v.getCurrentWaitMs(nowMs))
                .max().orElse(0L);
    }

    /** Has any emergency vehicle crossed the detection line? */
    public boolean hasDetectedEmergency() {
        return vehicles.stream().anyMatch(v -> v.isDetected() && v.isEmergency());
    }

    /** Lane density: 0.0 – 1.0 (based on total vehicle lengths / lane length) */
    public double getDensity() {
        double totalLen = vehicles.stream().mapToDouble(Vehicle::getLength).sum();
        return Math.min(1.0, totalLen / lengthMeters);
    }

    public double getDetectionThreshold() { return detectionThreshold; }

    // ---- Vehicle list management ----
    public synchronized void addVehicle(Vehicle v)    { vehicles.add(v); }
    public synchronized void removeVehicle(Vehicle v) { vehicles.remove(v); }
    public synchronized List<Vehicle> getVehicles()   { return new ArrayList<>(vehicles); }

    // ---- Getters ----
    public String getId()                    { return id; }
    public String getFromIntersectionId()    { return fromIntersectionId; }
    public String getToIntersectionId()      { return toIntersectionId; }
    public String getDirection()             { return direction; }
    public double getLengthMeters()          { return lengthMeters; }
    public boolean isBlocked()               { return blocked; }
    public void setBlocked(boolean blocked)  { this.blocked = blocked; }
}
