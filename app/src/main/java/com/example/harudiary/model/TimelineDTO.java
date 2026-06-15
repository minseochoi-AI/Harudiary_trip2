package com.example.harudiary.model;

import com.google.gson.annotations.SerializedName;

public class TimelineDTO {
    @SerializedName("id")
    private Long id;
    
    @SerializedName("date")
    private String date;
    
    @SerializedName("timeSlot")
    private String timeSlot;
    
    @SerializedName("isPlan")
    private boolean isPlan;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("photoUri")
    private String photoUri;
    
    @SerializedName("rating")
    private Float rating;
    
    @SerializedName("latitude")
    private Double latitude;
    
    @SerializedName("longitude")
    private Double longitude;
    
    @SerializedName("address")
    private String address;
    
    @SerializedName("weather")
    private String weather;
    
    @SerializedName("temperature")
    private Float temperature;
    
    @SerializedName("timestamp")
    private Long timestamp;
    
    @SerializedName("heartCount")
    private int heartCount;
    
    @SerializedName("commentCount")
    private int commentCount;
    
    @SerializedName("heartedByMe")
    private boolean heartedByMe;

    public Long getId() { return id; }
    public String getDate() { return date; }
    public String getTimeSlot() { return timeSlot; }
    public boolean isPlan() { return isPlan; }
    public String getContent() { return content; }
    public String getPhotoUri() { return photoUri; }
    public Float getRating() { return rating; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getAddress() { return address; }
    public String getWeather() { return weather; }
    public Float getTemperature() { return temperature; }
    public Long getTimestamp() { return timestamp; }
    public int getHeartCount() { return heartCount; }
    public int getCommentCount() { return commentCount; }
    public boolean isHeartedByMe() { return heartedByMe; }

    public void setHeartCount(int heartCount) { this.heartCount = heartCount; }
    public void setHeartedByMe(boolean heartedByMe) { this.heartedByMe = heartedByMe; }
}
