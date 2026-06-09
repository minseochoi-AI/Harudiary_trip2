package com.example.harudiary.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TravelPlanResponse {
    @SerializedName("trip_title")
    private String tripTitle;
    
    @SerializedName("days")
    private List<DayPlanDto> days;

    public String getTripTitle() { return tripTitle; }
    public void setTripTitle(String tripTitle) { this.tripTitle = tripTitle; }
    public List<DayPlanDto> getDays() { return days; }
    public void setDays(List<DayPlanDto> days) { this.days = days; }
}
