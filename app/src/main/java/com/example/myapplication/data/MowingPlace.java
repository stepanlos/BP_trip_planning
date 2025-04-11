package com.example.myapplication.data;

import java.util.List;

public class MowingPlace {

    private String id;
    private String name;
    private double timeRequirement;
    private int mowingCountPerYear;
    private int workCost;
    private List<String> visitDates;
    private String description;
    private List<DistanceEntry> distancesToOthers;
    private double latitude;
    private double longitude;
    private String caretaker;
    private String centre;
    private int area;
    private int locked;

    public static class DistanceEntry {
        private String id;
        private int distance;

        private int duration;

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
        public int getDuration() {
            return duration;
        }
        public void setDuration(int duration) {
            this.duration = duration;
        }
    }

    // getters and setters for all fields


    public String getCentre() {
        return centre;
    }

    public void setCentre(String centre) {
        this.centre = centre;
    }

    public String getCaretaker() {
        return caretaker;
    }

    public void setCaretaker(String caretaker) {
        this.caretaker = caretaker;
    }

    public int getArea() {
        return area;
    }

    public void setArea(int area) {
        this.area = area;
    }

    public int getLocked() {
        return locked;
    }

    public void setLocked(int locked) {
        this.locked = locked;
    }

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

    public double getTimeRequirement() {
        return timeRequirement;
    }

    public void setTimeRequirement(double timeRequirement) {
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
