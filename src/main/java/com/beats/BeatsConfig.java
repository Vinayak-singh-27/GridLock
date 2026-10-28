package com.beats;

import com.beats.simulation.SimulationEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeatsConfig {
    @Bean
    public SimulationEngine simulationEngine() {
        return new SimulationEngine();
    }
}
