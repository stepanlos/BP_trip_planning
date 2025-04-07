package com.example.myapplication.data;

import java.util.List;

/**
 * Model class representing a route plan.
 */
public class RoutePlan {
    private List<String> routeIds; // Ordered list of MowingPlace IDs
    private String date;          // Date when the route was planned
    private double speedMultiplier;
    private double totalTime;     // Total time (driving + mowing)
    private List<String> additionalInfo; // Any additional metadata

    // Getters and Setters

    public List<String> getRouteIds() {
        return routeIds;
    }

    public void setRouteIds(List<String> routeIds) {
        this.routeIds = routeIds;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void setSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    public double getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(double totalTime) {
        this.totalTime = totalTime;
    }

    public List<String> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(List<String> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}
