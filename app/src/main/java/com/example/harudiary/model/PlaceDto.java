package com.example.harudiary.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class PlaceDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("place_name")
    private String placeName;

    @SerializedName("travel_time_minutes_to_next")
    private int travelTimeMinutesToNext;

    @SerializedName("transport_mode")
    private String transportMode;

    @SerializedName("place_category")
    private String placeCategory;

    @SerializedName("time_spent_hours")
    private Double timeSpentHours;

    @SerializedName("place_url")
    private String placeUrl;

    @SerializedName("address_name")
    private String addressName;

    @SerializedName("x")
    private String x;

    @SerializedName("y")
    private String y;

    @SerializedName("day_number")
    private int dayNumber;

    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }
    public int getTravelTimeMinutesToNext() { return travelTimeMinutesToNext; }
    public void setTravelTimeMinutesToNext(int travelTimeMinutesToNext) { this.travelTimeMinutesToNext = travelTimeMinutesToNext; }
    public String getTransportMode() { return transportMode; }
    public void setTransportMode(String transportMode) { this.transportMode = transportMode; }
    public String getPlaceCategory() { return placeCategory; }
    public void setPlaceCategory(String placeCategory) { this.placeCategory = placeCategory; }

    public Double getTimeSpentHours() { return timeSpentHours; }
    public void setTimeSpentHours(Double timeSpentHours) { this.timeSpentHours = timeSpentHours; }

    public String getPlaceUrl() { return placeUrl; }
    public void setPlaceUrl(String placeUrl) { this.placeUrl = placeUrl; }

    public String getAddressName() { return addressName; }
    public void setAddressName(String addressName) { this.addressName = addressName; }

    public String getX() { return x; }
    public void setX(String x) { this.x = x; }

    public String getY() { return y; }
    public void setY(String y) { this.y = y; }

    public int getDayNumber() { return dayNumber; }
    public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }
}
