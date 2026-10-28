package com.beats.model;

/**
 * Per-lane detector that monitors vehicles crossing the detection threshold.
 * Decoupled from Lane so it can be extended with sensor models later.
 */
public class VehicleDetector {

    private final String id;
    private final Lane lane;

    public VehicleDetector(String id, Lane lane) {
        this.id = id;
        this.lane = lane;
    }

    /**
     * Called every simulation tick.  
     * Marks a vehicle as "detected" once its position passes the detection threshold.
     * A spawned vehicle at position 0.0 will NOT be detected until it has actually moved
     * past the detection line.
     */
    public void tick() {
        for (Vehicle v : lane.getVehicles()) {
            if (!v.isDetected() && v.getPositionOnLane() >= lane.getDetectionThreshold()) {
                v.setDetected(true);
            }
        }
    }

    public String getId()  { return id; }
    public Lane getLane()  { return lane; }

    /** Snapshot counts for dashboard */
    public int getDetectedCount()        { return lane.getDetectedCount(); }
    public int getDetectedWaitingCount() { return lane.getDetectedWaitingCount(); }
}
