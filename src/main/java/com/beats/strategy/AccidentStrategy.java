package com.beats.strategy;

import com.beats.model.*;

import java.util.List;

/**
 * Accident strategy: avoids blocked lanes, prioritises diversion roads.
 * Inherits normal adaptive logic but filters out blocked lanes first.
 */
public class AccidentStrategy extends NormalTrafficStrategy {

    public AccidentStrategy() {
        super(0.002, 1.5);
    }

    @Override
    protected TrafficSignal chooseBestSignal(List<TrafficSignal> signals, long nowMs) {
        // Prefer non-blocked lanes with the highest score
        TrafficSignal best = signals.stream()
                .filter(s -> !s.getControlledLane().isBlocked())
                .filter(s -> s.getControlledLane().getDetectedWaitingCount() > 0
                          || s.getControlledLane().hasDetectedEmergency())
                .max(java.util.Comparator.comparingDouble(s -> score(s, nowMs)))
                .orElse(null);
        return best;
    }

    @Override
    public String getModeName() { return "Accident"; }
}
