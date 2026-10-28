package com.beats.strategy;

import com.beats.model.*;

import java.util.List;
import java.util.Set;

/**
 * Festival/Procession strategy.
 * Lanes on the procession route are suppressed; surrounding roads get priority.
 */
public class FestivalStrategy extends NormalTrafficStrategy {

    private Set<String> processionLaneIds;

    public FestivalStrategy() {
        super(0.002, 1.5);
        this.processionLaneIds = Set.of();
    }

    public void setProcessionLaneIds(Set<String> ids) {
        this.processionLaneIds = ids;
    }

    @Override
    protected TrafficSignal chooseBestSignal(List<TrafficSignal> signals, long nowMs) {
        return signals.stream()
                .filter(s -> !s.getControlledLane().isBlocked())
                .filter(s -> !processionLaneIds.contains(s.getControlledLane().getId()))
                .filter(s -> s.getControlledLane().getDetectedWaitingCount() > 0
                          || s.getControlledLane().hasDetectedEmergency())
                .max(java.util.Comparator.comparingDouble(s -> score(s, nowMs)))
                .orElse(null);
    }

    @Override
    public String getModeName() { return "Festival"; }
}
