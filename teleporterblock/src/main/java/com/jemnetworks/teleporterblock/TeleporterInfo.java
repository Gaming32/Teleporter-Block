package com.jemnetworks.teleporterblock;

import java.util.Iterator;
import java.util.Map;

import org.bukkit.Location;

public class TeleporterInfo {
    protected final String name;
    protected Location location;
    private TeleporterInfo destination;
    private String preloadedDestinationName;
    private Map<String, TeleporterInfo> preloadedDestinations;
    protected int refCount;

    public TeleporterInfo(String name, Location location, TeleporterInfo destination) {
        this.name = name;
        this.location = location;
        this.destination = destination;
        this.refCount = 0;
    }

    protected TeleporterInfo(String name, Location location, String destination, Map<String, TeleporterInfo> possibleDestinations) {
        this.name = name;
        this.location = location;
        this.destination = null;
        if (possibleDestinations == null) {
            throw new IllegalStateException("possibleDestinations cannot be null");
        }
        this.preloadedDestinationName = destination;
        this.preloadedDestinations = possibleDestinations;
        this.refCount = 0;
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

    protected int calculateRefs(Iterator<TeleporterBlockInfo> blocks) {
        refCount = 0;
        for (TeleporterBlockInfo block : Utils.toIterable(blocks)) {
            if (block.teleporter == this) {
                refCount++;
            }
        }
        return refCount;
    }

    protected int ensureValid(Iterator<TeleporterBlockInfo> blocks) {
        refCount = 0;
        TeleporterBlockInfo firstBlock = null;
        boolean hasLocation = false;
        while (blocks.hasNext()) {
            TeleporterBlockInfo block = blocks.next();
            if (block.teleporter == this) {
                if (firstBlock == null) {
                    firstBlock = block;
                }
                if (block.location.equals(this.location)) {
                    hasLocation = true;
                }
                refCount++;
            }
        }
        if (!hasLocation && firstBlock != null) {
            this.location = firstBlock.location;
        }
        return refCount;
    }
}
