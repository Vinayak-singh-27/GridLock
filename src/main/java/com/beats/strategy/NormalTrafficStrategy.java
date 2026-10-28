package com.beats.strategy;

import com.beats.model.*;

import java.util.Comparator;
import java.util.List;

/**
 * Base adaptive strategy shared by Normal and RushHour modes.
 * Selects the highest-priority waiting lane for green; uses the
 * BEATS green-time formula: ceil(V * 2.2 + 3).
 */
public class NormalTrafficStrategy implements TrafficControlStrategy {

    protected final double waitWeightMs;   // how much to weight accumulated wait
    protected final double queueWeight;    // how much to weight queue size

    public NormalTrafficStrategy() {
        this(0.001, 1.0);
    }

    protected NormalTrafficStrategy(double waitWeightMs, double queueWeight) {
        this.waitWeightMs = waitWeightMs;
        this.queueWeight  = queueWeight;
    }

    @Override
    public void evaluateAndAssignGreen(Intersection intersection,
                                        List<TrafficSignal> signals,
                                        long nowMs) {
        // If any signal is already active (GREEN or YELLOW or ALL_RED), tick it
        boolean anyActive = signals.stream()
                .anyMatch(s -> s.getCurrentPhase() != TrafficSignal.Phase.RED);
        if (anyActive) {
            for (TrafficSignal s : signals) s.tick();
            return;
        }

        // All signals are RED → select the best candidate
        TrafficSignal best = chooseBestSignal(signals, nowMs);
        if (best == null) return;

        int detectedWaiting = best.getControlledLane().getDetectedWaitingCount();
        int greenSec = TrafficSignal.computeGreenTime(detectedWaiting);
        if (greenSec == 0) return; // no vehicles detected — skip

        best.grantGreen(greenSec);
        intersection.setCurrentGreenLaneId(best.getControlledLane().getId());
    }

    protected TrafficSignal chooseBestSignal(List<TrafficSignal> signals, long nowMs) {
        return signals.stream()
                .filter(s -> !s.getControlledLane().isBlocked())
                .filter(s -> s.getControlledLane().getDetectedWaitingCount() > 0
                          || s.getControlledLane().hasDetectedEmergency())
                .max(Comparator.comparingDouble(s -> score(s, nowMs)))
                .orElse(null);
    }

    protected double score(TrafficSignal signal, long nowMs) {
        Lane lane = signal.getControlledLane();
        int  dw   = lane.getDetectedWaitingCount();
        long wait = lane.getMaxWaitMs(nowMs);
        double emergencyBoost = lane.hasDetectedEmergency() ? 1000.0 : 0.0;
        return dw * queueWeight + wait * waitWeightMs + emergencyBoost;
    }

    @Override
    public String getModeName() { return "Normal"; }
}
