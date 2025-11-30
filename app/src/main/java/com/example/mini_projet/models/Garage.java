package com.example.mini_projet.models;

import com.google.firebase.firestore.GeoPoint;

public class  Garage {
    private String id;
    private String name;
    private String description;
    private GeoPoint address;
    private String phone;
    private String mechanicId;
    private boolean enabled;
    private String photoUrl;
    private float rating;
    private int reviewCount;

    // Default constructor (required for Firestore)
    public Garage() {
        this.rating = 1.0f; // Default rating
        this.reviewCount = 0;
    }

    // Constructor
    public Garage(String id, String name, String description, GeoPoint address, 
                  String phone, String mechanicId, String photoUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.address = address;
        this.phone = phone;
        this.mechanicId = mechanicId;
        this.photoUrl = photoUrl;
        this.enabled = false; // Default to false, requires admin approval
        this.rating = 1.0f; // Default rating
        this.reviewCount = 0;
    }

    // Getters and Setters
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

    public GeoPoint getAddress() {
        return address;
    }

    public void setAddress(GeoPoint address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMechanicId() {
        return mechanicId;
    }

    public void setMechanicId(String mechanicId) {
        this.mechanicId = mechanicId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }
}
