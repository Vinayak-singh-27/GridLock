package com.beats.model;

/**
 * Adaptive traffic signal for one incoming lane direction at an intersection.
 * Manages GREEN → YELLOW → ALL_RED transitions.
 */
public class TrafficSignal {

    public enum Phase { RED, GREEN, YELLOW, ALL_RED }

    // Config (seconds of sim time)
    public static final int MIN_GREEN_SEC  = 5;
    public static final int MAX_GREEN_SEC  = 60;
    public static final int YELLOW_SEC     = 3;
    public static final int ALL_RED_SEC    = 2;
    public static final double GREEN_TIME_COEFFICIENT = 2.2;
    public static final double GREEN_TIME_BASE        = 3.0;

    private final String id;
    private final Lane controlledLane;

    private Phase currentPhase;
    private int remainingTicks;   // sim ticks remaining in current phase (1 tick = 1 sim-second)
    private int currentGreenSec;

    public TrafficSignal(String id, Lane controlledLane) {
        this.id = id;
        this.controlledLane = controlledLane;
        this.currentPhase = Phase.RED;
        this.remainingTicks = 0;
        this.currentGreenSec = 0;
    }

    /** Formula: ceil(V * 2.2 + 3), clamped to [MIN, MAX]. Empty lane returns 0. */
    public static int computeGreenTime(int detectedWaiting) {
        if (detectedWaiting == 0) return 0;
        int raw = (int) Math.ceil(detectedWaiting * GREEN_TIME_COEFFICIENT + GREEN_TIME_BASE);
        return Math.max(MIN_GREEN_SEC, Math.min(MAX_GREEN_SEC, raw));
    }

    /** Grant green for the given number of seconds. */
    public void grantGreen(int seconds) {
        this.currentGreenSec = seconds;
        this.currentPhase = Phase.GREEN;
        this.remainingTicks = seconds;
    }

    /** Called every sim tick. Returns true when the phase just changed. */
    public boolean tick() {
        if (remainingTicks <= 0) return false;
        remainingTicks--;
        if (remainingTicks <= 0) {
            advancePhase();
            return true;
        }
        return false;
    }

    private void advancePhase() {
        switch (currentPhase) {
            case GREEN -> {
                currentPhase = Phase.YELLOW;
                remainingTicks = YELLOW_SEC;
            }
            case YELLOW -> {
                currentPhase = Phase.ALL_RED;
                remainingTicks = ALL_RED_SEC;
            }
            case ALL_RED -> {
                currentPhase = Phase.RED;
                remainingTicks = 0;
            }
            default -> {}
        }
    }

    public boolean isGreen()  { return currentPhase == Phase.GREEN; }
    public boolean isRed()    { return currentPhase == Phase.RED || currentPhase == Phase.ALL_RED; }

    public String getId()              { return id; }
    public Phase getCurrentPhase()     { return currentPhase; }
    public int getRemainingTicks()     { return remainingTicks; }
    public int getCurrentGreenSec()    { return currentGreenSec; }
    public Lane getControlledLane()    { return controlledLane; }
}
