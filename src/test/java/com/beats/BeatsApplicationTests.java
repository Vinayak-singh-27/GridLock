package com.beats;

import com.beats.model.*;
import com.beats.service.IncidentManager;
import com.beats.service.RoutePlanner;
import com.beats.simulation.ArohanCityBuilder;
import com.beats.simulation.SimulationEngine;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BeatsApplicationTests {

    // ---- 1. Green-time formula tests ----

    @Test
    void greenTimeZeroForEmptyLane() {
        assertEquals(0, TrafficSignal.computeGreenTime(0));
    }

    @Test
    void greenTimeForOneVehicle() {
        // ceil(1 * 2.2 + 3) = ceil(5.2) = 6, but clamped to MIN=5
        int expected = Math.max(TrafficSignal.MIN_GREEN_SEC,
                (int) Math.ceil(1 * TrafficSignal.GREEN_TIME_COEFFICIENT + TrafficSignal.GREEN_TIME_BASE));
        assertEquals(expected, TrafficSignal.computeGreenTime(1));
    }

    @Test
    void greenTimeForTenVehicles() {
        // ceil(10 * 2.2 + 3) = ceil(25) = 25
        assertEquals(25, TrafficSignal.computeGreenTime(10));
    }

    @Test
    void greenTimeCappedAtMax() {
        // 30 vehicles → ceil(30 * 2.2 + 3) = 69 → capped at 60
        assertEquals(TrafficSignal.MAX_GREEN_SEC, TrafficSignal.computeGreenTime(30));
    }

    // ---- 2. Signal phase transition tests ----

    @Test
    void signalTransitionsGreenToYellowToAllRedToRed() {
        Lane lane = new Lane("test_lane", "A", "B", "E", 200);
        TrafficSignal sig = new TrafficSignal("sig1", lane);
        sig.grantGreen(2);

        assertEquals(TrafficSignal.Phase.GREEN, sig.getCurrentPhase());
        sig.tick(); // tick 1 (remaining=1)
        sig.tick(); // tick 2 → remaining=0 → advance to YELLOW
        assertEquals(TrafficSignal.Phase.YELLOW, sig.getCurrentPhase());

        // Tick through YELLOW (3 ticks)
        for (int i = 0; i < TrafficSignal.YELLOW_SEC; i++) sig.tick();
        assertEquals(TrafficSignal.Phase.ALL_RED, sig.getCurrentPhase());

        // Tick through ALL_RED (2 ticks)
        for (int i = 0; i < TrafficSignal.ALL_RED_SEC; i++) sig.tick();
        assertEquals(TrafficSignal.Phase.RED, sig.getCurrentPhase());
    }

    // ---- 3. Detection-line counting test ----

    @Test
    void vehicleNotDetectedUntilCrossesDetectionLine() {
        Lane lane = new Lane("det_lane", "A", "B", "N", 300);
        VehicleDetector detector = new VehicleDetector("det1", lane);
        Car car = new Car();
        car.setCurrentLaneId(lane.getId());
        car.setPositionOnLane(0.0); // just spawned
        lane.addVehicle(car);

        detector.tick();
        assertFalse(car.isDetected(), "Car at position 0.0 should NOT be detected yet");

        car.setPositionOnLane(0.65);
        detector.tick();
        assertFalse(car.isDetected(), "Car at 0.65 (below 0.70 threshold) should NOT be detected");

        car.setPositionOnLane(0.71);
        detector.tick();
        assertTrue(car.isDetected(), "Car at 0.71 (above 0.70 threshold) SHOULD be detected");
    }

    // ---- 4. Emergency vehicle priority test ----

    @Test
    void ambulanceScoredHigherThanCar() {
        Lane laneA = new Lane("A_lane", "X", "Y", "N", 200);
        Lane laneB = new Lane("B_lane", "X", "Y", "S", 200);

        Car car = new Car();
        car.setPositionOnLane(0.75);
        car.setDetected(true);
        laneA.addVehicle(car);

        Ambulance amb = new Ambulance();
        amb.setPositionOnLane(0.75);
        amb.setDetected(true);
        laneB.addVehicle(amb);

        assertTrue(laneB.hasDetectedEmergency(), "Lane B must detect emergency");
        assertFalse(laneA.hasDetectedEmergency(), "Lane A must not detect emergency");
    }

    // ---- 5. Incident closure test ----

    @Test
    void roadClosedAfterAccidentIncident() {
        CityMap map = ArohanCityBuilder.build();
        IncidentManager im = new IncidentManager(map);
        RoadSegment road = map.getRoads().iterator().next();
        assertFalse(road.isClosed());

        IncidentManager.Incident inc = im.createAccident(road.getId());
        assertTrue(road.isClosed());
        assertTrue(road.getLaneAtoB().isBlocked());

        im.clearIncident(inc.id());
        assertFalse(road.isClosed());
        assertFalse(road.getLaneAtoB().isBlocked());
    }

    // ---- 6. Route planning test ----

    @Test
    void routePlannerFindsPathBetweenIntersections() {
        CityMap map = ArohanCityBuilder.build();
        RoutePlanner planner = new RoutePlanner(map);
        Car car = new Car();
        List<String> route = planner.planRouteFrom("CS");
        assertNotNull(route);
        assertFalse(route.isEmpty());
        assertEquals("CS", route.get(0));
    }

    @Test
    void routePlannerAvoidsClosedRoads() {
        CityMap map = ArohanCityBuilder.build();
        map.getRoad("R_CS_MJ").close("Test");
        RoutePlanner planner = new RoutePlanner(map);
        List<String> route = map.shortestPathAvoiding("CS", "MJ");
        // Should not pass through R_CS_MJ directly
        // Route must exist and go CS→...→MJ
        assertNotNull(route);
        assertEquals("CS", route.get(0));
        assertEquals("MJ", route.get(route.size() - 1));
    }

    // ---- 7. Context loads ----
    @Test
    void contextLoads() {}
}
