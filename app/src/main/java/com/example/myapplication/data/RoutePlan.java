package com.example.myapplication.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing a route plan.
 */
public class RoutePlan {
    // List of the names of all stops (excluding start and end)
    private List<MowingPlace> routePlaces;
    // URL for Mapy.cz route
    private String mapyCzUrl;
    // URL for Google Maps route
    private String googleMapsUrl;
    // Date and time when the route plan was created (format "yyyy-MM-dd HH:mm")
    private String dateTime;
    // Distance in meters
    private double length;
    // Duration in hours
    private double duration;

    // Getters and Setters
    public List<String> getRoutePlaces() {
        // Return the names of the route places
        List<String> names = new ArrayList<>();
        for (MowingPlace place : routePlaces) {
            names.add(place.getName());
        }
        return names;

    }

    public void setRoutePlaces(List<MowingPlace> routePlacesNames) {
        this.routePlaces = routePlacesNames;
    }

    public String getMapyCzUrl() {
        return mapyCzUrl;
    }

    public void setMapyCzUrl(String mapyCzUrl) {
        this.mapyCzUrl = mapyCzUrl;
    }

    public String getGoogleMapsUrl() {
        return googleMapsUrl;
    }

    public void setGoogleMapsUrl(String googleMapsUrl) {
        this.googleMapsUrl = googleMapsUrl;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public double getLength() {
        return length;
    }
    public void setLength(double length) {
        this.length = length;
    }
    public double getDuration() {
        return duration;
    }
    public void setDuration(double duration) {
        this.duration = duration;
    }
}
