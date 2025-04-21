package com.example.myapplication.ui.done;

public class VisitEntry {
    private final String placeId;
    private final String placeName;
    private final String visitDate; // formát "yyyy-MM-dd"

    public VisitEntry(String placeId, String placeName, String visitDate) {
        this.placeId = placeId;
        this.placeName = placeName;
        this.visitDate = visitDate;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getPlaceName() {
        return placeName;
    }

    public String getVisitDate() {
        return visitDate;
    }
}
