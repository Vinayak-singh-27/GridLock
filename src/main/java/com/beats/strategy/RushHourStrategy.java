package com.beats.strategy;

/**
 * Rush-Hour strategy: heavier weight on queue size and congestion.
 * Uses the same green-time formula but scores lanes more aggressively.
 */
public class RushHourStrategy extends NormalTrafficStrategy {

    public RushHourStrategy() {
        super(0.003, 2.0); // triple wait weight, double queue weight
    }

    @Override
    public String getModeName() { return "Rush Hour"; }
}
