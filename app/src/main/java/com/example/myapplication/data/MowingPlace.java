package com.example.myapplication.data;

import java.util.List;

public class MowingPlace {

    private String id;
    private String name;
    private int timeRequirement;
    private int mowingCountPerYear;
    private int workCost;
    private List<String> visitDates;
    private String description;
    private List<DistanceEntry> distancesToOthers;
    private double latitude;
    private double longitude;

    public static class DistanceEntry {
        private String id;
        private int distance;

        // getters and setters
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public int getDistance() {
            return distance;
        }
        public void setDistance(int distance) {
            this.distance = distance;
        }
    }

    // getters and setters for all fields


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTimeRequirement() {
        return timeRequirement;
    }

    public void setTimeRequirement(int timeRequirement) {
        this.timeRequirement = timeRequirement;
    }

    public int getMowingCountPerYear() {
        return mowingCountPerYear;
    }

    public void setMowingCountPerYear(int mowingCountPerYear) {
        this.mowingCountPerYear = mowingCountPerYear;
    }

    public int getWorkCost() {
        return workCost;
    }

    public void setWorkCost(int workCost) {
        this.workCost = workCost;
    }

    public List<String> getVisitDates() {
        return visitDates;
    }

    public void setVisitDates(List<String> visitDates) {
        this.visitDates = visitDates;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<DistanceEntry> getDistancesToOthers() {
        return distancesToOthers;
    }

    public void setDistancesToOthers(List<DistanceEntry> distancesToOthers) {
        this.distancesToOthers = distancesToOthers;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
