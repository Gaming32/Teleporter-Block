package com.jemnetworks.teleporterblock;

import java.util.Map;

import org.bukkit.Location;

public class TeleporterInfo {
    protected final String name;
    protected final Location location;
    private TeleporterInfo destination;
    private String preloadedDestinationName;
    private Map<String, TeleporterInfo> preloadedDestinations;

    public TeleporterInfo(String name, Location location, TeleporterInfo destination) {
        this.name = name;
        this.location = location;
        this.destination = destination;
    }

    protected TeleporterInfo(String name, Location location, String destination, Map<String, TeleporterInfo> possibleDestinations) {
        this.name = name;
        this.location = location;
        this.destination = null;
        if (preloadedDestinations == null) {
            throw new IllegalStateException("preloadedDestinations cannot be null");
        }
        this.preloadedDestinationName = destination;
        this.preloadedDestinations = possibleDestinations;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public TeleporterInfo getDestination() {
        if (destination == null && preloadedDestinations != null) {
            if (preloadedDestinations.containsKey(preloadedDestinationName)) {
                destination = preloadedDestinations.get(preloadedDestinationName);
                preloadedDestinations = null;
                preloadedDestinationName = null;
            }
        }
        return destination;
    }

    public void setDestination(TeleporterInfo destination) {
        this.destination = destination;
    }
}
