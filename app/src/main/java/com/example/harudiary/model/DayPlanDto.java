package com.example.harudiary.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class DayPlanDto implements Serializable {
    private static final long serialVersionUID = 1L;
    @SerializedName("day_number")
    private int dayNumber;

    @SerializedName("places")
    private List<PlaceDto> places;

    public int getDayNumber() { return dayNumber; }
    public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }
    public List<PlaceDto> getPlaces() { return places; }
    public void setPlaces(List<PlaceDto> places) { this.places = places; }
}
