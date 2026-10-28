package com.beats.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one road intersection (junction) in Arohan City.
 * An intersection holds a set of incoming lanes and their signals.
 */
public class Intersection {

    private final String id;
    private final String name;
    private final double mapX;   // pixel position in 2D view
    private final double mapY;

    private final List<Lane> incomingLanes;
    private final List<TrafficSignal> signals;
    private final List<VehicleDetector> detectors;

    private String currentGreenLaneId; // which lane currently has green

    public Intersection(String id, String name, double mapX, double mapY) {
        this.id = id;
        this.name = name;
        this.mapX = mapX;
        this.mapY = mapY;
        this.incomingLanes = new ArrayList<>();
        this.signals = new ArrayList<>();
        this.detectors = new ArrayList<>();
    }

    public void addIncomingLane(Lane lane) {
        incomingLanes.add(lane);
        TrafficSignal signal = new TrafficSignal("SIG_" + lane.getId(), lane);
        signals.add(signal);
        detectors.add(new VehicleDetector("DET_" + lane.getId(), lane));
    }

    public TrafficSignal getSignalForLane(String laneId) {
        return signals.stream()
                .filter(s -> s.getControlledLane().getId().equals(laneId))
                .findFirst().orElse(null);
    }

    public VehicleDetector getDetectorForLane(String laneId) {
        return detectors.stream()
                .filter(d -> d.getLane().getId().equals(laneId))
                .findFirst().orElse(null);
    }

    // ---- Getters ----
    public String getId()                          { return id; }
    public String getName()                        { return name; }
    public double getMapX()                        { return mapX; }
    public double getMapY()                        { return mapY; }
    public List<Lane> getIncomingLanes()           { return incomingLanes; }
    public List<TrafficSignal> getSignals()        { return signals; }
    public List<VehicleDetector> getDetectors()    { return detectors; }
    public String getCurrentGreenLaneId()          { return currentGreenLaneId; }
    public void setCurrentGreenLaneId(String id)   { this.currentGreenLaneId = id; }
}
