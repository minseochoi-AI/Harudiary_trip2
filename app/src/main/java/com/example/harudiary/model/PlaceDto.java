package com.example.harudiary.model;

import com.google.gson.annotations.SerializedName;

public class PlaceDto {
    @SerializedName("place_name")
    private String placeName;

    @SerializedName("travel_time_minutes_to_next")
    private int travelTimeMinutesToNext;

    @SerializedName("transport_mode")
    private String transportMode;

    @SerializedName("place_category")
    private String placeCategory;

    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }
    public int getTravelTimeMinutesToNext() { return travelTimeMinutesToNext; }
    public void setTravelTimeMinutesToNext(int travelTimeMinutesToNext) { this.travelTimeMinutesToNext = travelTimeMinutesToNext; }
    public String getTransportMode() { return transportMode; }
    public void setTransportMode(String transportMode) { this.transportMode = transportMode; }
    public String getPlaceCategory() { return placeCategory; }
    public void setPlaceCategory(String placeCategory) { this.placeCategory = placeCategory; }
}
