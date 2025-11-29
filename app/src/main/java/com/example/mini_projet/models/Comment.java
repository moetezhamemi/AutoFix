package com.example.mini_projet.models;

import java.util.Date;

public class Comment {
    private String userId;
    private String userName;
    private String text;
    private float rating;
    private Date timestamp;

    public Comment() {
        // Required for Firestore
    }

    public Comment(String userId, String userName, String text, float rating, Date timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.text = text;
        this.rating = rating;
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
