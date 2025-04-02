package com.minecraft.regionregen.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.*;

/**
 * Represents a region that can be regenerated
 */
public class Region implements ConfigurationSerializable {

    private String id;
    private String worldName;
    private Location minPoint;
    private Location maxPoint;
    private String creatorName;
    private Date creationDate;
    private Date lastRegenerationDate;
    private int regenerationCount;

    /**
     * Create a new region
     *
     * @param id Unique identifier for the region
     * @param worldName Name of the world the region is in
     * @param minPoint Minimum corner point of the region
     * @param maxPoint Maximum corner point of the region
     * @param creatorName Name of the player who created the region
     */
    public Region(String id, String worldName, Location minPoint, Location maxPoint, String creatorName) {
        this.id = id;
        this.worldName = worldName;
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
        this.creatorName = creatorName;
        this.creationDate = new Date();
        this.regenerationCount = 0;
    }

    /**
     * Create a region from a map (for deserialization)
     *
     * @param map The serialized map
     */
    @SuppressWarnings("unchecked")
    public Region(Map<String, Object> map) {
        this.id = (String) map.get("id");
        this.worldName = (String) map.get("world");
        this.creatorName = (String) map.get("creator");

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            throw new IllegalArgumentException("World '" + worldName + "' does not exist");
        }

        // Deserialize locations
        Map<String, Object> minPointMap = (Map<String, Object>) map.get("minPoint");
        Map<String, Object> maxPointMap = (Map<String, Object>) map.get("maxPoint");

        this.minPoint = new Location(
                world,
                (Double) minPointMap.get("x"),
                (Double) minPointMap.get("y"),
                (Double) minPointMap.get("z")
        );

        this.maxPoint = new Location(
                world,
                (Double) maxPointMap.get("x"),
                (Double) maxPointMap.get("y"),
                (Double) maxPointMap.get("z")
        );

        // Get dates
        this.creationDate = map.containsKey("creationDate") ?
                new Date((Long) map.get("creationDate")) : new Date();

        this.lastRegenerationDate = map.containsKey("lastRegenerationDate") ?
                new Date((Long) map.get("lastRegenerationDate")) : null;

        this.regenerationCount = map.containsKey("regenerationCount") ?
                (Integer) map.get("regenerationCount") : 0;
    }

    /**
     * Serialize the region to a map
     *
     * @return Serialized map of the region
     */
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("id", id);
        serialized.put("world", worldName);
        serialized.put("creator", creatorName);
        serialized.put("creationDate", creationDate.getTime());

        if (lastRegenerationDate != null) {
            serialized.put("lastRegenerationDate", lastRegenerationDate.getTime());
        }

        serialized.put("regenerationCount", regenerationCount);

        // Serialize locations
        Map<String, Object> minPointMap = new HashMap<>();
        minPointMap.put("x", minPoint.getX());
        minPointMap.put("y", minPoint.getY());
        minPointMap.put("z", minPoint.getZ());

        Map<String, Object> maxPointMap = new HashMap<>();
        maxPointMap.put("x", maxPoint.getX());
        maxPointMap.put("y", maxPoint.getY());
        maxPointMap.put("z", maxPoint.getZ());

        serialized.put("minPoint", minPointMap);
        serialized.put("maxPoint", maxPointMap);

        return serialized;
    }

    /**
     * Get the volume of the region (number of blocks)
     *
     * @return The volume of the region
     */
    public long getVolume() {
        long width = (long) (maxPoint.getX() - minPoint.getX() + 1);
        long height = (long) (maxPoint.getY() - minPoint.getY() + 1);
        long depth = (long) (maxPoint.getZ() - minPoint.getZ() + 1);

        return width * height * depth;
    }

    /**
     * Record that this region was regenerated
     */
    public void recordRegeneration() {
        this.lastRegenerationDate = new Date();
        this.regenerationCount++;
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorldName() {
        return worldName;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public Location getMinPoint() {
        return minPoint.clone();
    }

    public Location getMaxPoint() {
        return maxPoint.clone();
    }

    public String getCreatorName() {
        return creatorName;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Date getLastRegenerationDate() {
        return lastRegenerationDate;
    }

    public int getRegenerationCount() {
        return regenerationCount;
    }

    /**
     * Check if a location is within this region
     *
     * @param location The location to check
     * @return True if the location is within the region
     */
    public boolean contains(Location location) {
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }

        return location.getX() >= minPoint.getX() && location.getX() <= maxPoint.getX() &&
                location.getY() >= minPoint.getY() && location.getY() <= maxPoint.getY() &&
                location.getZ() >= minPoint.getZ() && location.getZ() <= maxPoint.getZ();
    }

    @Override
    public String toString() {
        return "Region{" +
                "id='" + id + '\'' +
                ", world='" + worldName + '\'' +
                ", volume=" + getVolume() +
                ", regenerations=" + regenerationCount +
                '}';
    }
}