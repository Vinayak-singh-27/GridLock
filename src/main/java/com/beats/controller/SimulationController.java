package com.beats.controller;

import com.beats.model.Vehicle;
import com.beats.service.IncidentManager;
import com.beats.simulation.SimulationEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API controller for all simulation commands.
 */
@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = "*")
public class SimulationController {

    private final SimulationEngine engine;

    public SimulationController(SimulationEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> start() {
        engine.start();
        return ok("Simulation started");
    }

    @PostMapping("/pause")
    public ResponseEntity<Map<String, String>> pause() {
        engine.pause();
        return ok("Simulation paused");
    }

    @PostMapping("/resume")
    public ResponseEntity<Map<String, String>> resume() {
        engine.resume();
        return ok("Simulation resumed");
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, String>> stop() {
        engine.stop();
        return ok("Simulation stopped");
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        engine.reset();
        return ok("Simulation reset");
    }

    @PostMapping("/mode/{mode}")
    public ResponseEntity<Map<String, String>> setMode(@PathVariable String mode) {
        try {
            SimulationEngine.SimMode m = SimulationEngine.SimMode.valueOf(mode.toUpperCase());
            engine.setMode(m);
            return ok("Mode set to " + m);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown mode: " + mode));
        }
    }

    @PostMapping("/speed/{multiplier}")
    public ResponseEntity<Map<String, String>> setSpeed(@PathVariable int multiplier) {
        engine.setSpeed(multiplier);
        return ok("Speed set to " + multiplier + "x");
    }

    @PostMapping("/spawn/{type}")
    public ResponseEntity<Map<String, String>> spawnVehicle(@PathVariable String type) {
        try {
            Vehicle.VehicleType vt = Vehicle.VehicleType.valueOf(type.toUpperCase());
            Vehicle v = engine.spawnVehicle(vt);
            if (v == null) return ResponseEntity.ok(Map.of("status", "failed", "message", "Could not spawn vehicle (no valid route)"));
            return ResponseEntity.ok(Map.of("status", "ok", "vehicleId", v.getId(), "message", type + " spawned"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown vehicle type: " + type));
        }
    }

    @PostMapping("/incident/accident/{roadId}")
    public ResponseEntity<Map<String, Object>> createAccident(@PathVariable String roadId) {
        try {
            IncidentManager.Incident inc = engine.createAccident(roadId);
            return ResponseEntity.ok(Map.of("status", "ok", "incidentId", inc.id(), "message", inc.description()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/incident/festival/{roadId}")
    public ResponseEntity<Map<String, Object>> createFestival(@PathVariable String roadId) {
        try {
            IncidentManager.Incident inc = engine.createFestival(roadId);
            return ResponseEntity.ok(Map.of("status", "ok", "incidentId", inc.id(), "message", inc.description()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/incident/{incidentId}")
    public ResponseEntity<Map<String, String>> clearIncident(@PathVariable String incidentId) {
        engine.clearIncident(incidentId);
        return ok("Incident " + incidentId + " cleared");
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
            "state",        engine.getState().name(),
            "mode",         engine.getMode().name(),
            "simTimeMs",    engine.getSimTimeMs(),
            "speed",        engine.getSpeedMultiplier(),
            "vehicleCount", engine.getVehicles().size()
        ));
    }

    @GetMapping("/roads")
    public ResponseEntity<Object> roads() {
        var roadList = engine.getCityMap().getRoads().stream().map(r -> Map.of(
            "id",     r.getId(),
            "from",   r.getIntersectionAId(),
            "to",     r.getIntersectionBId(),
            "closed", r.isClosed()
        )).toList();
        return ResponseEntity.ok(roadList);
    }

    private ResponseEntity<Map<String, String>> ok(String message) {
        return ResponseEntity.ok(Map.of("status", "ok", "message", message));
    }
}
