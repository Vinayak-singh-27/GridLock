package com.beats.service;

import com.beats.model.*;

import java.util.*;

/**
 * Manages incidents (accidents, road closures, festival processions).
 */
public class IncidentManager {

    public record Incident(String id, String type, String roadId, String description, long createdMs) {}

    private final Map<String, Incident> activeIncidents = new LinkedHashMap<>();
    private final CityMap cityMap;
    private int incidentCounter = 0;

    public IncidentManager(CityMap cityMap) {
        this.cityMap = cityMap;
    }

    public Incident createAccident(String roadId) {
        RoadSegment road = cityMap.getRoad(roadId);
        if (road == null) throw new IllegalArgumentException("Unknown road: " + roadId);
        road.close("Accident");
        String id = "INC-" + (++incidentCounter);
        Incident inc = new Incident(id, "ACCIDENT", roadId, "Accident on road " + roadId, System.currentTimeMillis());
        activeIncidents.put(id, inc);
        return inc;
    }

    public Incident createFestivalClosure(String roadId, String eventName) {
        RoadSegment road = cityMap.getRoad(roadId);
        if (road == null) throw new IllegalArgumentException("Unknown road: " + roadId);
        road.close("Festival: " + eventName);
        String id = "INC-" + (++incidentCounter);
        Incident inc = new Incident(id, "FESTIVAL", roadId, eventName + " procession on " + roadId, System.currentTimeMillis());
        activeIncidents.put(id, inc);
        return inc;
    }

    public void clearIncident(String incidentId) {
        Incident inc = activeIncidents.remove(incidentId);
        if (inc != null) {
            RoadSegment road = cityMap.getRoad(inc.roadId());
            if (road != null) road.open();
        }
    }

    public void clearAll() {
        new ArrayList<>(activeIncidents.keySet()).forEach(this::clearIncident);
    }

    public Collection<Incident> getActiveIncidents() { return activeIncidents.values(); }
}
