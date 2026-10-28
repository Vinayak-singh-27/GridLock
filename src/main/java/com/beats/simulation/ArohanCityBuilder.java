package com.beats.simulation;

import com.beats.model.*;

/**
 * Builds the Arohan City road network with three intersections.
 *
 *   Central Square (CS) ──── Market Junction (MJ) ──── Riverside Junction (RJ)
 *                │                                              │
 *                └──────────────────────────────────────────────┘
 *                          (South Ring Road)
 */
public class ArohanCityBuilder {

    public static CityMap build() {
        CityMap map = new CityMap();

        // ---- Intersections ----
        Intersection cs = new Intersection("CS", "Central Square",   300, 300);
        Intersection mj = new Intersection("MJ", "Market Junction",  600, 300);
        Intersection rj = new Intersection("RJ", "Riverside Junction", 600, 520);
        Intersection nr = new Intersection("NR", "North Ring",        300, 100);

        map.addIntersection(cs);
        map.addIntersection(mj);
        map.addIntersection(rj);
        map.addIntersection(nr);

        // ---- Roads (id, A, B, lengthMeters, dirAtoB, dirBtoA) ----
        RoadSegment mainStreet  = new RoadSegment("R_CS_MJ", "CS", "MJ", 400, "E", "W");
        RoadSegment riverside   = new RoadSegment("R_MJ_RJ", "MJ", "RJ", 350, "S", "N");
        RoadSegment southRing   = new RoadSegment("R_RJ_CS", "RJ", "CS", 450, "W", "E");
        RoadSegment northAvenue = new RoadSegment("R_CS_NR", "CS", "NR", 280, "N", "S");
        RoadSegment northEast   = new RoadSegment("R_NR_MJ", "NR", "MJ", 380, "SE","NW");

        map.addRoad(mainStreet);
        map.addRoad(riverside);
        map.addRoad(southRing);
        map.addRoad(northAvenue);
        map.addRoad(northEast);

        // ---- Register incoming lanes at each intersection ----
        // CS incoming: from MJ (W lane), from RJ (E lane of southRing), from NR (S lane)
        cs.addIncomingLane(mainStreet.getLaneBtoA());   // MJ→CS (W)
        cs.addIncomingLane(southRing.getLaneAtoB());    // RJ→CS (via southRing, direction W)
        cs.addIncomingLane(northAvenue.getLaneBtoA());  // NR→CS (S)

        // MJ incoming: from CS (E lane), from NR (SE lane), from RJ (N lane)
        mj.addIncomingLane(mainStreet.getLaneAtoB());   // CS→MJ (E)
        mj.addIncomingLane(northEast.getLaneAtoB());    // NR→MJ (SE)
        mj.addIncomingLane(riverside.getLaneBtoA());    // RJ→MJ (N)

        // RJ incoming: from MJ (S lane), from CS (E lane of southRing)
        rj.addIncomingLane(riverside.getLaneAtoB());    // MJ→RJ (S)
        rj.addIncomingLane(southRing.getLaneBtoA());    // CS→RJ (E)

        // NR incoming: from CS (N lane), from MJ (NW lane)
        nr.addIncomingLane(northAvenue.getLaneAtoB());  // CS→NR (N)
        nr.addIncomingLane(northEast.getLaneBtoA());    // MJ→NR (NW)

        return map;
    }
}
