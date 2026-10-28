package com.beats.model;

import java.util.UUID;

/**
 * Abstract base class for all vehicles in the BEATS simulation.
 * Demonstrates OOP abstraction and inheritance.
 */
public abstract class Vehicle {

    public enum VehicleType { CAR, AMBULANCE, BUS, TRUCK }

    protected final String id;
    protected final VehicleType type;
    protected final double lengthMeters;
    protected final double maxSpeedMps;   // meters per sim-second
    protected final int priority;         // higher = more priority

    // Route / position state
    protected java.util.List<String> route;      // ordered list of intersection IDs
    protected int routeIndex;                    // current segment in route
    protected String currentLaneId;
    protected double positionOnLane;             // 0.0 = entry, 1.0 = exit of lane
    protected double currentSpeedMps;

    // Detection / waiting state
    protected boolean detected;          // has crossed the detection line?
    protected boolean waitingAtRed;
    protected long waitStartMs;
    protected long totalWaitMs;

    // Simulation flags
    protected boolean active;
    protected boolean finished;

    protected Vehicle(VehicleType type, double lengthMeters, double maxSpeedMps, int priority) {
        this.id = type.name().charAt(0) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        this.type = type;
        this.lengthMeters = lengthMeters;
        this.maxSpeedMps = maxSpeedMps;
        this.priority = priority;
        this.currentSpeedMps = maxSpeedMps;
        this.active = false;
        this.finished = false;
        this.detected = false;
        this.waitingAtRed = false;
        this.positionOnLane = 0.0;
        this.routeIndex = 0;
    }

    // ---- Abstract behaviour ----
    public abstract String getDisplayColor();
    public abstract boolean isEmergency();

    // ---- Getters ----
    public String getId()               { return id; }
    public VehicleType getType()        { return type; }
    public double getLength()           { return lengthMeters; }
    public double getMaxSpeed()         { return maxSpeedMps; }
    public int getPriority()            { return priority; }
    public java.util.List<String> getRoute() { return route; }
    public int getRouteIndex()          { return routeIndex; }
    public String getCurrentLaneId()    { return currentLaneId; }
    public double getPositionOnLane()   { return positionOnLane; }
    public double getCurrentSpeed()     { return currentSpeedMps; }
    public boolean isDetected()         { return detected; }
    public boolean isWaitingAtRed()     { return waitingAtRed; }
    public long getTotalWaitMs()        { return totalWaitMs; }
    public boolean isActive()           { return active; }
    public boolean isFinished()         { return finished; }

    // ---- Setters / mutators ----
    public void setRoute(java.util.List<String> route) {
        this.route = route;
        this.routeIndex = 0;
    }
    public void setCurrentLaneId(String laneId)       { this.currentLaneId = laneId; }
    public void setPositionOnLane(double pos)          { this.positionOnLane = Math.max(0.0, Math.min(1.0, pos)); }
    public void setCurrentSpeed(double speed)          { this.currentSpeedMps = Math.max(0, Math.min(maxSpeedMps, speed)); }
    public void setDetected(boolean detected)          { this.detected = detected; }
    public void setActive(boolean active)              { this.active = active; }
    public void setFinished(boolean finished)          { this.finished = finished; }
    public void advanceRouteIndex()                    { this.routeIndex++; }

    public void startWaiting(long nowMs) {
        if (!waitingAtRed) {
            waitingAtRed = true;
            waitStartMs = nowMs;
        }
    }

    public void stopWaiting(long nowMs) {
        if (waitingAtRed) {
            totalWaitMs += (nowMs - waitStartMs);
            waitingAtRed = false;
        }
    }

    public long getCurrentWaitMs(long nowMs) {
        return waitingAtRed ? (nowMs - waitStartMs) : 0L;
    }

    @Override
    public String toString() {
        return String.format("%s[id=%s, lane=%s, pos=%.2f]", type, id, currentLaneId, positionOnLane);
    }
}
