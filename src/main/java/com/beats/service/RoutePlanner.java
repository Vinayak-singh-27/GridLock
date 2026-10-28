package com.beats.service;

import com.beats.model.*;

import java.util.*;

/**
 * Plans routes for vehicles across the city map.
 * Uses BFS; respects road closures when possible.
 */
public class RoutePlanner {

    private final CityMap cityMap;
    private final Random rng = new Random();

    public RoutePlanner(CityMap cityMap) {
        this.cityMap = cityMap;
    }

    /**
     * Assign a random origin/destination route to a vehicle (avoiding closed roads).
     * The route is a list of intersection IDs the vehicle should traverse.
     */
    public List<String> planRandomRoute(Vehicle vehicle) {
        List<String> intersectionIds = new ArrayList<>(
                cityMap.getIntersections().stream().map(Intersection::getId).toList());
        if (intersectionIds.size() < 2) return Collections.emptyList();

        String from = intersectionIds.get(rng.nextInt(intersectionIds.size()));
        String to;
        do { to = intersectionIds.get(rng.nextInt(intersectionIds.size())); } while (to.equals(from));

        return cityMap.shortestPathAvoiding(from, to);
    }

    /**
     * Plan route from a specific intersection to a random destination.
     */
    public List<String> planRouteFrom(String fromId) {
        List<String> intersectionIds = new ArrayList<>(
                cityMap.getIntersections().stream().map(Intersection::getId).toList());
        if (intersectionIds.size() < 2) return List.of(fromId);

        String to;
        do { to = intersectionIds.get(rng.nextInt(intersectionIds.size())); } while (to.equals(fromId));

        return cityMap.shortestPathAvoiding(fromId, to);
    }

    /**
     * Reroute a vehicle that is currently on a blocked lane.
     * Returns the new route or null if no alternative exists.
     */
    public List<String> rerouteVehicle(Vehicle vehicle) {
        List<String> route = vehicle.getRoute();
        if (route == null || vehicle.getRouteIndex() >= route.size()) return null;

        String currentIntersection = route.get(vehicle.getRouteIndex());
        String destination = route.get(route.size() - 1);

        if (currentIntersection.equals(destination)) return null;
        return cityMap.shortestPathAvoiding(currentIntersection, destination);
    }

    public CityMap getCityMap() { return cityMap; }
}
