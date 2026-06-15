package com.example.harudiary.model;

/**
 * FriendRequest — 친구 요청 모델
 */
public class FriendRequest {
    private long id;
    private User fromUser;
    private User toUser;
    private String status;       // "pending", "accepted", "rejected"
    private String createdAt;

    public FriendRequest() {}

    public long getRequestId() { return id; }
    public void setRequestId(long id) { this.id = id; }

    public String getFromUserName() { 
        return fromUser != null && fromUser.getName() != null ? fromUser.getName() : "알 수 없음"; 
    }

    public String getFromUserEmail() { 
        return fromUser != null && fromUser.getUserId() != null ? fromUser.getUserId() : ""; 
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
