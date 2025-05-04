package com.example.myapplication.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing a route plan.
 */
public class RoutePlan {
    /**
     * List of mowing places included in the route.
     * Each place is represented by an instance of MowingPlace.
     */
    private List<MowingPlace> routePlaces;

    /**
     * URL for the route on Mapy.cz
     */
    private String mapyCzUrl;

    /**
     * URL for the route on Google Maps
     */
    private String googleMapsUrl;

    /**
     * Date and time of the route in the format "yyyy-MM-dd HH:mm:ss"
     */
    private String dateTime;

    /**
     * Length of the route in meters.
     */
    private double length;

    /**
     * Duration of the route in seconds.
     */
    private double duration;

    /**
     * geter for routePlaces
     * @return List of MowingPlace objects representing the route places
     */
    public List<String> getRoutePlaces() {
        // Return the names of the route places
        List<String> names = new ArrayList<>();
        for (MowingPlace place : routePlaces) {
            names.add(place.getName());
        }
        return names;

    }

    /**
     * Setter for routePlaces
     * @param routePlacesNames List of MowingPlace objects to set as the route places
     */
    public void setRoutePlaces(List<MowingPlace> routePlacesNames) {
        this.routePlaces = routePlacesNames;
    }

    /**
     * Getter for mapyCzUrl
     * @return String representing the URL for the route on Mapy.cz
     */
    public String getMapyCzUrl() {
        return mapyCzUrl;
    }

    /**
     * Setter for mapyCzUrl
     * @param mapyCzUrl String representing the URL to set for the route on Mapy.cz
     */
    public void setMapyCzUrl(String mapyCzUrl) {
        this.mapyCzUrl = mapyCzUrl;
    }

    /**
     * Getter for googleMapsUrl
     * @return String representing the URL for the route on Google Maps
     */
    public String getGoogleMapsUrl() {
        return googleMapsUrl;
    }

    /**
     * Setter for googleMapsUrl
     * @param googleMapsUrl String representing the URL to set for the route on Google Maps
     */
    public void setGoogleMapsUrl(String googleMapsUrl) {
        this.googleMapsUrl = googleMapsUrl;
    }

    /**
     * Getter for dateTime
     * @return String representing the date and time of the route
     */
    public String getDateTime() {
        return dateTime;
    }

    /**
     * Setter for dateTime
     * @param dateTime String representing the date and time to set for the route
     */
    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    /**
     * Getter for length
     * @return double representing the length of the route in meters
     */
    public double getLength() {
        return length;
    }

    /**
     * Setter for length
     * @param length double representing the length to set for the route in meters
     */
    public void setLength(double length) {
        this.length = length;
    }

    /**
     * Getter for duration
     * @return double representing the duration of the route in seconds
     */
    public double getDuration() {
        return duration;
    }

    /**
     * Setter for duration
     * @param duration double representing the duration to set for the route in seconds
     */
    public void setDuration(double duration) {
        this.duration = duration;
    }
}
