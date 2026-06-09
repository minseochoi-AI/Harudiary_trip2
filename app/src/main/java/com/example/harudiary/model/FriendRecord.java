package com.example.harudiary.model;

/**
 * FriendRecord — 친구의 활동 기록 (사용자 이름 포함)
 */
public class FriendRecord extends Record {
    private String userName;
    private String latestActivity;

    public FriendRecord() {}

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getLatestActivity() {
        return latestActivity;
    }

    public void setLatestActivity(String latestActivity) {
        this.latestActivity = latestActivity;
    }
}
