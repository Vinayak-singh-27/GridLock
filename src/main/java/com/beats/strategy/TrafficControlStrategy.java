package com.beats.strategy;

import com.beats.model.Intersection;
import com.beats.model.TrafficSignal;

import java.util.List;

/**
 * Strategy interface for adaptive traffic-control algorithms.
 * Each mode (Normal, RushHour, Accident, Festival) provides its own implementation.
 */
public interface TrafficControlStrategy {

    /**
     * Called once per intersection per simulation tick.
     * Chooses which signal should receive green next and for how long.
     *
     * @param intersection the intersection to evaluate
     * @param signals      all signals at this intersection
     * @param nowMs        current simulation wall-clock ms
     */
    void evaluateAndAssignGreen(Intersection intersection, List<TrafficSignal> signals, long nowMs);

    /** Human-readable name of the strategy. */
    String getModeName();
}
