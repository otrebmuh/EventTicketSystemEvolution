package com.eventbooking.event.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "Event Service");
        info.put("version", "1.0.0");
        info.put("status", "running");
        info.put("endpoints", Map.of(
                "events", "/api/events",
                "categories", "/api/categories",
                "search", "/api/events/search",
                "health", "/actuator/health"));
        return info;
    }
}
