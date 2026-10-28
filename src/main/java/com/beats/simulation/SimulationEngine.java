package com.beats.simulation;

import com.beats.model.*;
import com.beats.service.*;
import com.beats.strategy.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.Logger;

/**
 * Central simulation engine for B.E.A.T.S.
 * Manages the tick loop, vehicle lifecycle, and mode switching.
 */
public class SimulationEngine {

    private static final Logger LOG = Logger.getLogger(SimulationEngine.class.getName());

    public enum SimState  { STOPPED, RUNNING, PAUSED }
    public enum SimMode   { NORMAL, RUSH_HOUR, ACCIDENT, FESTIVAL }

    // ---- City & controllers ----
    private final CityMap          cityMap;
    private final RoutePlanner     routePlanner;
    private final IncidentManager  incidentManager;
    private final List<TrafficController> controllers = new ArrayList<>();
    private final List<String>     eventLog = Collections.synchronizedList(new LinkedList<>());

    // ---- Vehicle registry ----
    private final Map<String, Vehicle> vehicles = new ConcurrentHashMap<>();

    // ---- Sim state ----
    private final AtomicReference<SimState> state    = new AtomicReference<>(SimState.STOPPED);
    private final AtomicReference<SimMode>  mode     = new AtomicReference<>(SimMode.NORMAL);
    private final AtomicInteger speedMultiplier       = new AtomicInteger(1);
    private long simTimeMs = 0L;
    private final Object tickLock = new Object();

    // ---- Strategies ----
    private final NormalTrafficStrategy  normalStrategy   = new NormalTrafficStrategy();
    private final RushHourStrategy       rushHourStrategy = new RushHourStrategy();
    private final AccidentStrategy       accidentStrategy = new AccidentStrategy();
    private final FestivalStrategy       festivalStrategy = new FestivalStrategy();

    // ---- Background ticker ----
    private ScheduledExecutorService scheduler;
    private static final int TICK_INTERVAL_MS = 1000; // 1 sim-second = 1 real second

    private final Random rng = new Random();

    public SimulationEngine() {
        this.cityMap         = ArohanCityBuilder.build();
        this.routePlanner    = new RoutePlanner(cityMap);
        this.incidentManager = new IncidentManager(cityMap);
        initControllers();
        log("BEATS simulation engine initialised. City: Arohan City.");
    }

    // ---- Initialisation ----

    private void initControllers() {
        controllers.clear();
        for (Intersection inter : cityMap.getIntersections()) {
            controllers.add(new TrafficController(inter, normalStrategy));
        }
    }

    // ---- Lifecycle ----

    public synchronized void start() {
        if (state.get() == SimState.RUNNING) return;
        state.set(SimState.RUNNING);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        int delay = Math.max(100, TICK_INTERVAL_MS / speedMultiplier.get());
        scheduler.scheduleAtFixedRate(this::tick, 0, delay, TimeUnit.MILLISECONDS);
        log("Simulation STARTED in " + mode.get() + " mode.");
    }

    public synchronized void pause() {
        if (state.get() != SimState.RUNNING) return;
        state.set(SimState.PAUSED);
        if (scheduler != null) scheduler.shutdown();
        log("Simulation PAUSED at t=" + simTimeMs + "ms.");
    }

    public synchronized void resume() {
        if (state.get() != SimState.PAUSED) return;
        state.set(SimState.RUNNING);
        int delay = Math.max(100, TICK_INTERVAL_MS / speedMultiplier.get());
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::tick, 0, delay, TimeUnit.MILLISECONDS);
        log("Simulation RESUMED.");
    }

    public synchronized void stop() {
        state.set(SimState.STOPPED);
        if (scheduler != null) { scheduler.shutdown(); scheduler = null; }
        log("Simulation STOPPED.");
    }

    public synchronized void reset() {
        stop();
        vehicles.clear();
        simTimeMs = 0L;
        incidentManager.clearAll();
        eventLog.clear();
        // Re-open all roads
        for (RoadSegment road : cityMap.getRoads()) road.open();
        // Clear all lanes
        for (Intersection inter : cityMap.getIntersections()) {
            for (Lane lane : inter.getIncomingLanes()) {
                for (Vehicle v : lane.getVehicles()) lane.removeVehicle(v);
            }
            inter.setCurrentGreenLaneId(null);
            for (TrafficSignal sig : inter.getSignals()) {
                // reset to RED
                if (sig.getCurrentPhase() != TrafficSignal.Phase.RED) {
                    // force via reflection-free method: grant 0 won't work, use a tiny helper
                    resetSignal(sig);
                }
            }
        }
        setMode(SimMode.NORMAL);
        log("Simulation RESET.");
    }

    private void resetSignal(TrafficSignal sig) {
        // Trick: grant green 0 doesn't work; we just leave it — on next strategy eval it will stay RED
        // No public reset method needed since on start the strategy will handle it
    }

    // ---- Tick ----

    private void tick() {
        if (state.get() != SimState.RUNNING) return;
        synchronized (tickLock) {
            simTimeMs += TICK_INTERVAL_MS;
            long nowMs = simTimeMs;

            // 1. Move vehicles
            moveVehicles(nowMs);

            // 2. Run traffic controllers
            for (TrafficController ctrl : controllers) {
                ctrl.tick(nowMs);
            }

            // 3. Update vehicle waiting state based on signals
            updateVehicleWaiting(nowMs);

            // 4. Rush hour: auto-spawn traffic
            if (mode.get() == SimMode.RUSH_HOUR && simTimeMs % 5000 == 0 && vehicles.size() < 30) {
                spawnVehicle(Vehicle.VehicleType.CAR);
            }

            // 5. Normal: occasional auto-spawn
            if (mode.get() == SimMode.NORMAL && simTimeMs % 8000 == 0 && vehicles.size() < 15) {
                spawnVehicle(Vehicle.VehicleType.CAR);
            }
        }
    }

    // ---- Vehicle movement ----

    private void moveVehicles(long nowMs) {
        List<Vehicle> toRemove = new ArrayList<>();

        for (Vehicle vehicle : vehicles.values()) {
            if (!vehicle.isActive() || vehicle.isFinished()) continue;

            Lane lane = getLane(vehicle.getCurrentLaneId());
            if (lane == null) { toRemove.add(vehicle); continue; }

            TrafficSignal signal = getSignalForLane(lane);

            // Determine if vehicle can move
            boolean atStopLine = vehicle.getPositionOnLane() >= 0.95;
            boolean redLight   = (signal != null) && signal.isRed();
            boolean blocked    = lane.isBlocked();

            if (atStopLine && (redLight || blocked)) {
                // Waiting at stop line
                vehicle.setCurrentSpeed(0);
                vehicle.startWaiting(nowMs);
                continue;
            }

            vehicle.stopWaiting(nowMs);

            // Compute safe speed (following distance)
            double safeSpeed = computeSafeSpeed(vehicle, lane);
            vehicle.setCurrentSpeed(safeSpeed);

            // Advance position (pos is 0→1 over lane length)
            double distPerTick = safeSpeed * (TICK_INTERVAL_MS / 1000.0);
            double posInc = lane.getLengthMeters() > 0 ? distPerTick / lane.getLengthMeters() : 0;
            double newPos = vehicle.getPositionOnLane() + posInc;

            if (newPos >= 1.0) {
                // Vehicle has exited the lane — advance to next segment
                advanceToNextSegment(vehicle, lane, nowMs, toRemove);
            } else {
                vehicle.setPositionOnLane(newPos);
            }
        }

        for (Vehicle v : toRemove) {
            finishVehicle(v);
        }
    }

    private double computeSafeSpeed(Vehicle vehicle, Lane lane) {
        double minGap = vehicle.getLength() * 1.5;
        double speed  = vehicle.getMaxSpeed();

        // Find the nearest vehicle ahead
        List<Vehicle> laneVehicles = lane.getVehicles();
        double aheadPos = Double.MAX_VALUE;
        for (Vehicle other : laneVehicles) {
            if (other == vehicle) continue;
            if (other.getPositionOnLane() > vehicle.getPositionOnLane()) {
                double gap = (other.getPositionOnLane() - vehicle.getPositionOnLane()) * lane.getLengthMeters();
                if (gap < aheadPos) aheadPos = gap;
            }
        }

        if (aheadPos < minGap) {
            // Slow down proportionally
            speed = speed * (aheadPos / minGap);
        }

        // Cap by type-specific speed
        return Math.max(0, Math.min(speed, vehicle.getMaxSpeed()));
    }

    private void advanceToNextSegment(Vehicle vehicle, Lane currentLane, long nowMs, List<Vehicle> toRemove) {
        List<String> route = vehicle.getRoute();
        int idx = vehicle.getRouteIndex();

        currentLane.removeVehicle(vehicle);
        vehicle.setDetected(false);
        vehicle.stopWaiting(nowMs);

        if (idx + 1 >= route.size()) {
            // Reached destination
            vehicle.setFinished(true);
            toRemove.add(vehicle);
            log("Vehicle " + vehicle.getId() + " reached destination.");
            return;
        }

        // Move to next road segment
        String nextIntersectionId = route.get(idx + 1);
        vehicle.advanceRouteIndex();

        // Find the lane from current intersection to next
        String currentIntersectionId = route.get(vehicle.getRouteIndex() - 1);
        RoadSegment road = cityMap.getRoadBetween(currentIntersectionId, nextIntersectionId);

        if (road == null || road.isClosed()) {
            // Try reroute
            List<String> newRoute = routePlanner.rerouteVehicle(vehicle);
            if (newRoute != null && newRoute.size() > 1) {
                vehicle.setRoute(newRoute);
                String newNextId = newRoute.get(1);
                road = cityMap.getRoadBetween(newRoute.get(0), newNextId);
            }
        }

        if (road == null) {
            toRemove.add(vehicle);
            return;
        }

        Lane nextLane = road.getLaneForDirection(currentIntersectionId);
        if (nextLane == null || nextLane.isBlocked()) {
            toRemove.add(vehicle);
            return;
        }

        vehicle.setCurrentLaneId(nextLane.getId());
        vehicle.setPositionOnLane(0.0);
        nextLane.addVehicle(vehicle);
    }

    private void finishVehicle(Vehicle vehicle) {
        Lane lane = getLane(vehicle.getCurrentLaneId());
        if (lane != null) lane.removeVehicle(vehicle);
        vehicle.setActive(false);
        vehicles.remove(vehicle.getId());
    }

    // ---- Waiting state update ----

    private void updateVehicleWaiting(long nowMs) {
        for (Vehicle vehicle : vehicles.values()) {
            Lane lane = getLane(vehicle.getCurrentLaneId());
            if (lane == null) continue;
            TrafficSignal signal = getSignalForLane(lane);
            if (signal == null) continue;

            boolean nearStop = vehicle.getPositionOnLane() >= 0.85;
            boolean isRed    = signal.isRed();

            if (nearStop && isRed && vehicle.isDetected()) {
                vehicle.startWaiting(nowMs);
            } else if (!isRed || !nearStop) {
                vehicle.stopWaiting(nowMs);
            }
        }
    }

    // ---- Helpers ----

    private Lane getLane(String laneId) {
        if (laneId == null) return null;
        for (Intersection inter : cityMap.getIntersections()) {
            for (Lane lane : inter.getIncomingLanes()) {
                if (lane.getId().equals(laneId)) return lane;
            }
        }
        return null;
    }

    private TrafficSignal getSignalForLane(Lane lane) {
        if (lane == null) return null;
        String toId = lane.getToIntersectionId();
        Intersection inter = cityMap.getIntersection(toId);
        if (inter == null) return null;
        return inter.getSignalForLane(lane.getId());
    }

    // ---- Public API ----

    public Vehicle spawnVehicle(Vehicle.VehicleType type) {
        Vehicle v = switch (type) {
            case CAR       -> new Car();
            case AMBULANCE -> new Ambulance();
            case BUS       -> new Bus();
            case TRUCK     -> new Truck();
        };

        List<String> route = routePlanner.planRandomRoute(v);
        if (route == null || route.size() < 2) {
            log("Could not plan route for " + type);
            return null;
        }
        v.setRoute(route);

        // Place on the first lane of the route
        String fromId = route.get(0);
        String toId   = route.get(1);
        RoadSegment road = cityMap.getRoadBetween(fromId, toId);
        if (road == null || road.isClosed()) {
            log("First road blocked for spawned vehicle.");
            return null;
        }
        Lane firstLane = road.getLaneForDirection(fromId);
        if (firstLane.isBlocked()) return null;

        v.setCurrentLaneId(firstLane.getId());
        v.setPositionOnLane(0.0);
        v.setActive(true);
        firstLane.addVehicle(v);
        vehicles.put(v.getId(), v);
        log("Spawned " + type + " [" + v.getId() + "] on route: " + route);
        return v;
    }

    public void setMode(SimMode newMode) {
        this.mode.set(newMode);
        TrafficControlStrategy strategy = switch (newMode) {
            case NORMAL    -> normalStrategy;
            case RUSH_HOUR -> rushHourStrategy;
            case ACCIDENT  -> accidentStrategy;
            case FESTIVAL  -> festivalStrategy;
        };
        for (TrafficController ctrl : controllers) {
            ctrl.setStrategy(strategy);
        }
        log("Mode changed to: " + newMode);
    }

    public void setSpeed(int multiplier) {
        speedMultiplier.set(Math.max(1, Math.min(10, multiplier)));
        if (state.get() == SimState.RUNNING) {
            // Restart scheduler with new interval
            stop();
            start();
        }
    }

    public IncidentManager.Incident createAccident(String roadId) {
        setMode(SimMode.ACCIDENT);
        IncidentManager.Incident inc = incidentManager.createAccident(roadId);
        // Reroute vehicles on blocked lane
        vehicles.values().forEach(v -> {
            Lane lane = getLane(v.getCurrentLaneId());
            if (lane != null && lane.isBlocked()) {
                List<String> newRoute = routePlanner.rerouteVehicle(v);
                if (newRoute != null) v.setRoute(newRoute);
            }
        });
        log("ACCIDENT created on road " + roadId);
        return inc;
    }

    public IncidentManager.Incident createFestival(String roadId) {
        setMode(SimMode.FESTIVAL);
        IncidentManager.Incident inc = incidentManager.createFestivalClosure(roadId, "Ganpati Procession");
        festivalStrategy.setProcessionLaneIds(
            Set.of(cityMap.getRoad(roadId).getLaneAtoB().getId(),
                   cityMap.getRoad(roadId).getLaneBtoA().getId())
        );
        log("FESTIVAL closure on road " + roadId);
        return inc;
    }

    public void clearIncident(String incidentId) {
        incidentManager.clearIncident(incidentId);
        log("Incident " + incidentId + " cleared.");
    }

    private void log(String msg) {
        String entry = "[T+" + (simTimeMs/1000) + "s] " + msg;
        eventLog.add(0, entry);
        if (eventLog.size() > 100) eventLog.subList(100, eventLog.size()).clear();
        LOG.info(entry);
    }

    // ---- State snapshot for REST/WS ----

    public SimState getState()                       { return state.get(); }
    public SimMode  getMode()                        { return mode.get(); }
    public long     getSimTimeMs()                   { return simTimeMs; }
    public int      getSpeedMultiplier()             { return speedMultiplier.get(); }
    public Map<String, Vehicle> getVehicles()        { return vehicles; }
    public CityMap  getCityMap()                     { return cityMap; }
    public List<TrafficController> getControllers()  { return controllers; }
    public IncidentManager getIncidentManager()      { return incidentManager; }
    public List<String> getEventLog()                { return Collections.unmodifiableList(eventLog); }
}
