package com.example.harudiary.model;

import com.google.gson.annotations.SerializedName;

public class Record {
    @SerializedName("id")
    private Long activityId;
    private String userId;
    private String date;       // YYYY-MM-DD
    private String timeSlot;   // morning / lunch / evening
    private String content;
    private String photoUri;
    private float rating;
    private double latitude;
    private double longitude;
    private String address;
    private String weather;
    private float temperature;
    private long timestamp;
    private boolean isPlan;

    public Record() {}

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getPhotoUri() { return photoUri; }
    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getWeather() { return weather; }
    public void setWeather(String weather) { this.weather = weather; }

    public float getTemperature() { return temperature; }
    public void setTemperature(float temperature) { this.temperature = temperature; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isPlan() { return isPlan; }
    public void setPlan(boolean plan) { isPlan = plan; }
}
