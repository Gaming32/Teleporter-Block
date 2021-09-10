package com.jemnetworks.teleporterblock;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

public class TeleporterBlockInfo {
    protected final Location location;
    protected final TeleporterInfo teleporter;
    protected final UUID displayEntity;

    public TeleporterBlockInfo(Location location, TeleporterInfo teleporter, UUID displayEntity) {
        this.location = location;
        this.teleporter = teleporter;
        this.displayEntity = displayEntity;
    }

    public Location getLocation() {
        return location;
    }

    public World getWorld() {
        return location.getWorld();
    }

    public TeleporterInfo getTeleporter() {
        return teleporter;
    }

    public UUID getDisplayEntity() {
        return displayEntity;
    }
}
