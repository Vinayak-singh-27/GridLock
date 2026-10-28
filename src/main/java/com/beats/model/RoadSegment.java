package com.beats.model;

/**
 * A bidirectional road connecting two intersections.
 * Composed of two Lane objects (one per direction).
 */
public class RoadSegment {

    private final String id;
    private final String intersectionAId;
    private final String intersectionBId;
    private final Lane laneAtoB;
    private final Lane laneBtoA;
    private final double lengthMeters;

    private boolean closed;
    private String closureReason;

    public RoadSegment(String id, String intersectionAId, String intersectionBId,
                       double lengthMeters, String directionAtoB, String directionBtoA) {
        this.id = id;
        this.intersectionAId = intersectionAId;
        this.intersectionBId = intersectionBId;
        this.lengthMeters = lengthMeters;
        this.laneAtoB = new Lane(id + "_AB", intersectionAId, intersectionBId, directionAtoB, lengthMeters);
        this.laneBtoA = new Lane(id + "_BA", intersectionBId, intersectionAId, directionBtoA, lengthMeters);
        this.closed = false;
    }

    public Lane getLaneForDirection(String fromId) {
        return fromId.equals(intersectionAId) ? laneAtoB : laneBtoA;
    }

    public void close(String reason) {
        this.closed = true;
        this.closureReason = reason;
        laneAtoB.setBlocked(true);
        laneBtoA.setBlocked(true);
    }

    public void open() {
        this.closed = false;
        this.closureReason = null;
        laneAtoB.setBlocked(false);
        laneBtoA.setBlocked(false);
    }

    public String getId()              { return id; }
    public String getIntersectionAId() { return intersectionAId; }
    public String getIntersectionBId() { return intersectionBId; }
    public Lane getLaneAtoB()          { return laneAtoB; }
    public Lane getLaneBtoA()          { return laneBtoA; }
    public double getLengthMeters()    { return lengthMeters; }
    public boolean isClosed()          { return closed; }
    public String getClosureReason()   { return closureReason; }
}
