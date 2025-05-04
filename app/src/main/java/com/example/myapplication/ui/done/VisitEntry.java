package com.example.myapplication.ui.done;

/**
 * Represents a visit entry with details about the place visited and the date of the visit.
 * This class is used to display completed visits in the DoneFragment.
 */
public class VisitEntry {
    private final String placeId;
    private final String placeName;
    private final String visitDate; // formatted as "yyyy-MM-dd"

    /**
     * Constructor for VisitEntry.
     *
     * @param placeId   The ID of the place visited.
     * @param placeName The name of the place visited.
     * @param visitDate The date of the visit in "yyyy-MM-dd" format.
     */
    public VisitEntry(String placeId, String placeName, String visitDate) {
        this.placeId = placeId;
        this.placeName = placeName;
        this.visitDate = visitDate;
    }

    /**
     * Gets the ID of the place visited.
     *
     * @return The ID of the place.
     */
    public String getPlaceId() {
        return placeId;
    }

    /**
     * Gets the name of the place visited.
     *
     * @return The name of the place.
     */
    public String getPlaceName() {
        return placeName;
    }

    /**
     * Gets the date of the visit.
     *
     * @return The date of the visit in "yyyy-MM-dd" format.
     */
    public String getVisitDate() {
        return visitDate;
    }
}
