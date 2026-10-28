package com.beats.websocket;

import com.beats.simulation.SimulationEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * WebSocket handler that broadcasts simulation state to all connected clients.
 */
@Component
public class SimulationWebSocketHandler extends TextWebSocketHandler {

    private final SimulationEngine engine;
    private final ObjectMapper     objectMapper;
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    public SimulationWebSocketHandler(SimulationEngine engine, ObjectMapper objectMapper) {
        this.engine       = engine;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    /** Push state snapshot every 500 ms. */
    @Scheduled(fixedRate = 500)
    public void broadcastState() {
        if (sessions.isEmpty()) return;
        try {
            String json = objectMapper.writeValueAsString(buildSnapshot());
            TextMessage msg = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try { session.sendMessage(msg); }
                    catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            // log silently
        }
    }

    private Map<String, Object> buildSnapshot() {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("simTimeMs",   engine.getSimTimeMs());
        snap.put("state",       engine.getState().name());
        snap.put("mode",        engine.getMode().name());
        snap.put("speed",       engine.getSpeedMultiplier());
        snap.put("vehicleCount", engine.getVehicles().size());

        // Vehicles
        List<Map<String, Object>> vehicleList = new ArrayList<>();
        for (var entry : engine.getVehicles().entrySet()) {
            var v = entry.getValue();
            Map<String, Object> vm = new LinkedHashMap<>();
            vm.put("id",         v.getId());
            vm.put("type",       v.getType().name());
            vm.put("laneId",     v.getCurrentLaneId());
            vm.put("pos",        v.getPositionOnLane());
            vm.put("speed",      v.getCurrentSpeed());
            vm.put("detected",   v.isDetected());
            vm.put("waiting",    v.isWaitingAtRed());
            vm.put("color",      v.getDisplayColor());
            vm.put("emergency",  v.isEmergency());
            vm.put("route",      v.getRoute());
            vm.put("routeIdx",   v.getRouteIndex());
            vehicleList.add(vm);
        }
        snap.put("vehicles", vehicleList);

        // Intersections
        List<Map<String, Object>> interList = new ArrayList<>();
        for (var inter : engine.getCityMap().getIntersections()) {
            Map<String, Object> im = new LinkedHashMap<>();
            im.put("id",   inter.getId());
            im.put("name", inter.getName());
            im.put("x",    inter.getMapX());
            im.put("y",    inter.getMapY());
            im.put("greenLaneId", inter.getCurrentGreenLaneId());

            List<Map<String, Object>> signalList = new ArrayList<>();
            for (var sig : inter.getSignals()) {
                Map<String, Object> sm = new LinkedHashMap<>();
                sm.put("laneId",    sig.getControlledLane().getId());
                sm.put("phase",     sig.getCurrentPhase().name());
                sm.put("remaining", sig.getRemainingTicks());
                sm.put("greenSec",  sig.getCurrentGreenSec());
                sm.put("direction", sig.getControlledLane().getDirection());
                sm.put("queueLen",  sig.getControlledLane().getQueueLength());
                sm.put("detected",  sig.getControlledLane().getDetectedWaitingCount());
                sm.put("density",   sig.getControlledLane().getDensity());
                sm.put("blocked",   sig.getControlledLane().isBlocked());
                sm.put("emergency", sig.getControlledLane().hasDetectedEmergency());
                signalList.add(sm);
            }
            im.put("signals", signalList);
            interList.add(im);
        }
        snap.put("intersections", interList);

        // Roads
        List<Map<String, Object>> roadList = new ArrayList<>();
        for (var road : engine.getCityMap().getRoads()) {
            Map<String, Object> rm = new LinkedHashMap<>();
            rm.put("id",     road.getId());
            rm.put("from",   road.getIntersectionAId());
            rm.put("to",     road.getIntersectionBId());
            rm.put("closed", road.isClosed());
            rm.put("reason", road.getClosureReason());
            roadList.add(rm);
        }
        snap.put("roads", roadList);

        // Incidents
        List<Map<String, Object>> incList = new ArrayList<>();
        for (var inc : engine.getIncidentManager().getActiveIncidents()) {
            Map<String, Object> inm = new LinkedHashMap<>();
            inm.put("id",          inc.id());
            inm.put("type",        inc.type());
            inm.put("roadId",      inc.roadId());
            inm.put("description", inc.description());
            incList.add(inm);
        }
        snap.put("incidents", incList);

        // Event log (last 20)
        List<String> log = engine.getEventLog();
        snap.put("eventLog", log.subList(0, Math.min(20, log.size())));

        return snap;
    }
}
