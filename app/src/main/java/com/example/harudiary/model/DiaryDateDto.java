package com.example.harudiary.model;

import com.google.gson.annotations.SerializedName;

public class DiaryDateDto {
    private String date;
    
    @SerializedName(value = "plan", alternate = {"isPlan"})
    private Boolean isPlan;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Boolean getIsPlan() { return isPlan; }
    public void setIsPlan(Boolean plan) { isPlan = plan; }
}
