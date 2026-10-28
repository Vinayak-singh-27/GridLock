package com.beats.service;

import com.beats.model.*;
import com.beats.strategy.TrafficControlStrategy;

import java.util.*;

/**
 * Per-intersection traffic controller.
 * Delegates green-time decisions to the active TrafficControlStrategy.
 */
public class TrafficController {

    private final Intersection intersection;
    private TrafficControlStrategy strategy;

    public TrafficController(Intersection intersection, TrafficControlStrategy strategy) {
        this.intersection = intersection;
        this.strategy = strategy;
    }

    public void setStrategy(TrafficControlStrategy strategy) {
        this.strategy = strategy;
    }

    /** Called every simulation tick. */
    public void tick(long nowMs) {
        // Run detectors
        for (VehicleDetector det : intersection.getDetectors()) {
            det.tick();
        }
        // Let strategy decide
        strategy.evaluateAndAssignGreen(intersection, intersection.getSignals(), nowMs);
    }

    public Intersection getIntersection() { return intersection; }
    public TrafficControlStrategy getStrategy() { return strategy; }
}
