package com.example.sitesync.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "projects")
public class ProjectEntity implements Serializable {

    @PrimaryKey
    @NonNull
    private String id;
    private String projectCode;
    private String name;
    private String clientName;
    private String contractorName;
    private String location;
    private String budget;
    private String activeFloor;
    private String status; // ACTIVE, COMPLETED, ON_HOLD
    private long createdAt;
    private long lastOpenedAt;

    // Project Scope & Plan Specifications (Mandatory for project initialization)
    private String buildingType; // e.g. "2-Room Residential Building", "Commercial Multi-Storey"
    private int totalRooms; // e.g. 2
    private float wallHeightFeet; // e.g. 10.0f
    private int numWindows; // e.g. 6
    private int numDoors; // e.g. 4
    private String flooringType; // e.g. "Tiles" or "Mosaic"
    private String planDetailsJson; // Comprehensive scope specifications

    // Progress metrics
    private float overallProgressPercent; // 0.0 - 100.0
    private float civilProgressPercent; // Masonry & Casting
    private float electricProgressPercent; // Electrical & Wiring
    private float plumbingProgressPercent; // Sanitary & Piping
    private float thaiGlassProgressPercent; // Thai Aluminum Glass
    private float tilesFlooringProgressPercent; // Tiles & Flooring
    private float interiorPlasterProgressPercent; // Plaster & Interior
    private float exteriorRoofProgressPercent; // Roof & Exterior

    public ProjectEntity() {
        this.id = "";
        this.status = "ACTIVE";
        this.createdAt = System.currentTimeMillis();
        this.lastOpenedAt = System.currentTimeMillis();
    }

    public ProjectEntity(@NonNull String id, String projectCode, String name, String clientName,
                         String contractorName, String location, String budget, String activeFloor,
                         String buildingType, int totalRooms, float wallHeightFeet, int numWindows,
                         int numDoors, String flooringType, String planDetailsJson,
                         float overallProgressPercent) {
        this.id = id;
        this.projectCode = projectCode;
        this.name = name;
        this.clientName = clientName;
        this.contractorName = contractorName;
        this.location = location;
        this.budget = budget;
        this.activeFloor = activeFloor;
        this.status = "ACTIVE";
        this.createdAt = System.currentTimeMillis();
        this.lastOpenedAt = System.currentTimeMillis();
        this.buildingType = buildingType;
        this.totalRooms = totalRooms;
        this.wallHeightFeet = wallHeightFeet;
        this.numWindows = numWindows;
        this.numDoors = numDoors;
        this.flooringType = flooringType;
        this.planDetailsJson = planDetailsJson;
        this.overallProgressPercent = overallProgressPercent;

        // Default trade distribution
        this.civilProgressPercent = 100f;
        this.electricProgressPercent = 45f;
        this.plumbingProgressPercent = 40f;
        this.thaiGlassProgressPercent = 20f;
        this.tilesFlooringProgressPercent = 15f;
        this.interiorPlasterProgressPercent = 50f;
        this.exteriorRoofProgressPercent = 30f;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getContractorName() { return contractorName; }
    public void setContractorName(String contractorName) { this.contractorName = contractorName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }

    public String getActiveFloor() { return activeFloor; }
    public void setActiveFloor(String activeFloor) { this.activeFloor = activeFloor; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getLastOpenedAt() { return lastOpenedAt; }
    public void setLastOpenedAt(long lastOpenedAt) { this.lastOpenedAt = lastOpenedAt; }

    public String getBuildingType() { return buildingType; }
    public void setBuildingType(String buildingType) { this.buildingType = buildingType; }

    public int getTotalRooms() { return totalRooms; }
    public void setTotalRooms(int totalRooms) { this.totalRooms = totalRooms; }

    public float getWallHeightFeet() { return wallHeightFeet; }
    public void setWallHeightFeet(float wallHeightFeet) { this.wallHeightFeet = wallHeightFeet; }

    public int getNumWindows() { return numWindows; }
    public void setNumWindows(int numWindows) { this.numWindows = numWindows; }

    public int getNumDoors() { return numDoors; }
    public void setNumDoors(int numDoors) { this.numDoors = numDoors; }

    public String getFlooringType() { return flooringType; }
    public void setFlooringType(String flooringType) { this.flooringType = flooringType; }

    public String getPlanDetailsJson() { return planDetailsJson; }
    public void setPlanDetailsJson(String planDetailsJson) { this.planDetailsJson = planDetailsJson; }

    public float getOverallProgressPercent() { return overallProgressPercent; }
    public void setOverallProgressPercent(float overallProgressPercent) { this.overallProgressPercent = overallProgressPercent; }

    public float getCivilProgressPercent() { return civilProgressPercent; }
    public void setCivilProgressPercent(float civilProgressPercent) { this.civilProgressPercent = civilProgressPercent; }

    public float getElectricProgressPercent() { return electricProgressPercent; }
    public void setElectricProgressPercent(float electricProgressPercent) { this.electricProgressPercent = electricProgressPercent; }

    public float getPlumbingProgressPercent() { return plumbingProgressPercent; }
    public void setPlumbingProgressPercent(float plumbingProgressPercent) { this.plumbingProgressPercent = plumbingProgressPercent; }

    public float getThaiGlassProgressPercent() { return thaiGlassProgressPercent; }
    public void setThaiGlassProgressPercent(float thaiGlassProgressPercent) { this.thaiGlassProgressPercent = thaiGlassProgressPercent; }

    public float getTilesFlooringProgressPercent() { return tilesFlooringProgressPercent; }
    public void setTilesFlooringProgressPercent(float tilesFlooringProgressPercent) { this.tilesFlooringProgressPercent = tilesFlooringProgressPercent; }

    public float getInteriorPlasterProgressPercent() { return interiorPlasterProgressPercent; }
    public void setInteriorPlasterProgressPercent(float interiorPlasterProgressPercent) { this.interiorPlasterProgressPercent = interiorPlasterProgressPercent; }

    public float getExteriorRoofProgressPercent() { return exteriorRoofProgressPercent; }
    public void setExteriorRoofProgressPercent(float exteriorRoofProgressPercent) { this.exteriorRoofProgressPercent = exteriorRoofProgressPercent; }
}
