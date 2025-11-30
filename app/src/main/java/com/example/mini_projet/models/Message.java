package com.example.mini_projet.models;

public class Message {
    private String senderId;
    private String text;
    private long timestamp;
    private String type; // "text" or "location"
    private Double latitude;
    private Double longitude;

    public Message() {
        // Required for Firestore
    }

    public Message(String senderId, String text, long timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
        this.type = "text";
    }

    public Message(String senderId, String text, long timestamp, String type, Double latitude, Double longitude) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
