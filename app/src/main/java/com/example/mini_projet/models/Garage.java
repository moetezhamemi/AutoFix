package com.example.mini_projet.models;

import com.google.firebase.firestore.GeoPoint;
import java.util.ArrayList;
import java.util.List;

public class Garage {
    private String id;
    private String name;
    private String description;
    private String phoneNumber;
    private String imageUrl;
    private GeoPoint location;
    private float rating;
    private List<Comment> comments;
    private boolean enabled;

    public Garage() {
        // Required for Firestore
        this.enabled = false; // Default to false as requested
        this.comments = new ArrayList<>();
    }

    public Garage(String id, String name, String description, String phoneNumber, String imageUrl, GeoPoint location) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.phoneNumber = phoneNumber;
        this.imageUrl = imageUrl;
        this.location = location;
        this.enabled = false;
        this.comments = new ArrayList<>();
        this.rating = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
