package com.example.harudiary.model;

public class ActivityComment {
    private int    commentId;
    private int    fromUserId;
    private int    activityId;
    private String fromName;
    private String content;
    private long   createdAt;

    public int    getCommentId()   { return commentId; }
    public void   setCommentId(int v)   { commentId = v; }
    public int    getFromUserId()  { return fromUserId; }
    public void   setFromUserId(int v)  { fromUserId = v; }
    public int    getActivityId()  { return activityId; }
    public void   setActivityId(int v)  { activityId = v; }
    public String getFromName()    { return fromName; }
    public void   setFromName(String v) { fromName = v; }
    public String getContent()     { return content; }
    public void   setContent(String v)  { content = v; }
    public long   getCreatedAt()   { return createdAt; }
    public void   setCreatedAt(long v)  { createdAt = v; }
}
