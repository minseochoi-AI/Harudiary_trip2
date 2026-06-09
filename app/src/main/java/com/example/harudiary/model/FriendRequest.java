package com.example.harudiary.model;

/**
 * FriendRequest — 친구 요청 모델
 */
public class FriendRequest {
    private int requestId;
    private int fromUserId;
    private int toUserId;
    private String status;       // "pending", "accepted", "rejected"
    private long createdAt;
    private String fromUserName;  // JOIN 결과
    private String fromUserEmail; // JOIN 결과

    public FriendRequest() {}

    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }

    public int getFromUserId() { return fromUserId; }
    public void setFromUserId(int fromUserId) { this.fromUserId = fromUserId; }

    public int getToUserId() { return toUserId; }
    public void setToUserId(int toUserId) { this.toUserId = toUserId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getFromUserName() { return fromUserName; }
    public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }

    public String getFromUserEmail() { return fromUserEmail; }
    public void setFromUserEmail(String fromUserEmail) { this.fromUserEmail = fromUserEmail; }
}
