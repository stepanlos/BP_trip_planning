package com.example.myapplication.data;

import java.util.List;

/**
 * Represents a mowing place with various attributes such as location,
 * time requirements, and distances to other places.
 */
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

    /**
     * Represents a distance entry to another mowing place.
     */
    public static class DistanceEntry {

        private String id;
        private int distance;

        private int duration;

        // getters and setters
        /**
         * Gets the ID of the other mowing place.
         * @return The ID of the other place.
         */
        public String getId() {
            return id;
        }

        /**
         * Sets the ID of the other mowing place.
         * @param id The ID to set.
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * Gets the distance to the other mowing place.
         * @return The distance in meters.
         */
        public int getDistance() {
            return distance;
        }

        /**
         * Sets the distance to the other mowing place.
         * @param distance The distance in meters.
         */
        public void setDistance(int distance) {
            this.distance = distance;
        }

        /**
         * Gets the duration to the other mowing place.
         * @return The duration in seconds.
         */
        public int getDuration() {
            return duration;
        }

        /**
         * Sets the duration to the other mowing place.
         * @param duration The duration in seconds.
         */
        public void setDuration(int duration) {
            this.duration = duration;
        }
    }

    // getters and setters for all fields

    /**
     * Gets the centre of the mowing place.
     * @return The centre.
     */
    public String getCentre() {
        return centre;
    }

    /**
     * Sets the centre of the mowing place.
     * @param centre The centre to set.
     */
    public void setCentre(String centre) {
        this.centre = centre;
    }

    /**
     * Gets the caretaker of the mowing place.
     * @return The caretaker.
     */
    public String getCaretaker() {
        return caretaker;
    }

    /**
     * Sets the caretaker of the mowing place.
     * @param caretaker The caretaker to set.
     */
    public void setCaretaker(String caretaker) {
        this.caretaker = caretaker;
    }

    /**
     * Gets the area of the mowing place.
     * @return The area in square meters.
     */
    public int getArea() {
        return area;
    }

    /**
     * Sets the area of the mowing place.
     * @param area The area in square meters.
     */
    public void setArea(int area) {
        this.area = area;
    }

    /**
     * Gets the locked status of the mowing place.
     * @return The locked status.
     */
    public int getLocked() {
        return locked;
    }

    /**
     * Sets the locked status of the mowing place.
     * @param locked The locked status to set.
     */
    public void setLocked(int locked) {
        this.locked = locked;
    }

    /**
     * Gets the ID of the mowing place.
     * @return The ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of the mowing place.
     * @param id The ID to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the name of the mowing place.
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the mowing place.
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the time requirement for the mowing place.
     * @return The time requirement in hours.
     */
    public double getTimeRequirement() {
        return timeRequirement;
    }

    /**
     * Sets the time requirement for the mowing place.
     * @param timeRequirement The time requirement in hours.
     */
    public void setTimeRequirement(double timeRequirement) {
        this.timeRequirement = timeRequirement;
    }

    /**
     * Gets the count of mowings per year.
     * @return The count of mowings.
     */
    public int getMowingCountPerYear() {
        return mowingCountPerYear;
    }

    /**
     * Sets the count of mowings per year.
     * @param mowingCountPerYear The count of mowings to set.
     */
    public void setMowingCountPerYear(int mowingCountPerYear) {
        this.mowingCountPerYear = mowingCountPerYear;
    }

    /**
     * Gets the work cost for the mowing place.
     * @return The work cost in currency units.
     */
    public int getWorkCost() {
        return workCost;
    }

    /**
     * Sets the work cost for the mowing place.
     * @param workCost The work cost in currency units.
     */
    public void setWorkCost(int workCost) {
        this.workCost = workCost;
    }

    /**
     * Gets the list of visit dates for the mowing place.
     * @return The list of visit dates.
     */
    public List<String> getVisitDates() {
        return visitDates;
    }

    /**
     * Sets the list of visit dates for the mowing place.
     * @param visitDates The list of visit dates to set.
     */
    public void setVisitDates(List<String> visitDates) {
        this.visitDates = visitDates;
    }

    /**
     * Gets the description of the mowing place.
     * @return The description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the mowing place.
     * @param description The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the list of distances to other mowing places.
     * @return The list of distance entries.
     */
    public List<DistanceEntry> getDistancesToOthers() {
        return distancesToOthers;
    }

    /**
     * Sets the list of distances to other mowing places.
     * @param distancesToOthers The list of distance entries to set.
     */
    public void setDistancesToOthers(List<DistanceEntry> distancesToOthers) {
        this.distancesToOthers = distancesToOthers;
    }

    /**
     * Gets the latitude of the mowing place.
     * @return The latitude.
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Sets the latitude of the mowing place.
     * @param latitude The latitude to set.
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Gets the longitude of the mowing place.
     * @return The longitude.
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Sets the longitude of the mowing place.
     * @param longitude The longitude to set.
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
