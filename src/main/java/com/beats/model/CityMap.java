package com.beats.model;

import java.util.*;

/**
 * The map of Arohan City.
 * Contains all intersections and road segments.
 * Provides graph-traversal helpers for route planning.
 */
public class CityMap {

    private final Map<String, Intersection> intersections = new LinkedHashMap<>();
    private final Map<String, RoadSegment>  roads         = new LinkedHashMap<>();
    // adjacency: intersection id → list of (neighbor id, road id)
    private final Map<String, List<String[]>> adjacency   = new HashMap<>();

    public void addIntersection(Intersection intersection) {
        intersections.put(intersection.getId(), intersection);
        adjacency.putIfAbsent(intersection.getId(), new ArrayList<>());
    }

    public void addRoad(RoadSegment road) {
        roads.put(road.getId(), road);
        // bidirectional adjacency
        adjacency.computeIfAbsent(road.getIntersectionAId(), k -> new ArrayList<>())
                 .add(new String[]{road.getIntersectionBId(), road.getId()});
        adjacency.computeIfAbsent(road.getIntersectionBId(), k -> new ArrayList<>())
                 .add(new String[]{road.getIntersectionAId(), road.getId()});
    }

    /** BFS shortest path (ignoring blocked roads). Returns ordered intersection IDs. */
    public List<String> shortestPath(String fromId, String toId) {
        return bfs(fromId, toId, false);
    }

    /** BFS that respects road closures. Returns ordered intersection IDs. */
    public List<String> shortestPathAvoiding(String fromId, String toId) {
        return bfs(fromId, toId, true);
    }

    private List<String> bfs(String fromId, String toId, boolean avoidClosed) {
        if (fromId.equals(toId)) return List.of(fromId);
        Map<String, String> parent = new HashMap<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(fromId);
        parent.put(fromId, null);

        while (!queue.isEmpty()) {
            String cur = queue.poll();
            for (String[] edge : adjacency.getOrDefault(cur, Collections.emptyList())) {
                String neighbor = edge[0];
                String roadId   = edge[1];
                if (parent.containsKey(neighbor)) continue;
                RoadSegment road = roads.get(roadId);
                if (avoidClosed && road != null && road.isClosed()) continue;
                parent.put(neighbor, cur);
                if (neighbor.equals(toId)) return buildPath(parent, fromId, toId);
                queue.add(neighbor);
            }
        }
        // fallback: no open path — return direct path regardless
        return bfs(fromId, toId, false);
    }

    private List<String> buildPath(Map<String, String> parent, String from, String to) {
        LinkedList<String> path = new LinkedList<>();
        String cur = to;
        while (cur != null) { path.addFirst(cur); cur = parent.get(cur); }
        return path;
    }

    /** Find which RoadSegment connects two adjacent intersections. */
    public RoadSegment getRoadBetween(String aId, String bId) {
        for (String[] edge : adjacency.getOrDefault(aId, Collections.emptyList())) {
            if (edge[0].equals(bId)) return roads.get(edge[1]);
        }
        return null;
    }

    public Intersection getIntersection(String id)           { return intersections.get(id); }
    public RoadSegment   getRoad(String id)                  { return roads.get(id); }
    public Collection<Intersection> getIntersections()       { return intersections.values(); }
    public Collection<RoadSegment>  getRoads()               { return roads.values(); }
    public Map<String, List<String[]>> getAdjacency()        { return adjacency; }
}
