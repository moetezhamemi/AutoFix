package com.example.mini_projet.models;

import com.google.firebase.firestore.GeoPoint;

public class User {
    private String id;  // Firebase Auth UID (auto-set)
    private String name;
    private String email;
    private GeoPoint address;  // Latitude/Longitude for map location
    private String photoUrl;  // URL from Firebase Storage
    private String phone;
    private String role;  // "admin", "client", "mechanic"
    private boolean enabled; // true if client by default, false otherwise

    // Default constructor (required for Firestore)
    public User() {}

    // Constructor with all fields
    public User(String id, String name, String email, GeoPoint address, String photoUrl, String phone, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.address = address;
        this.photoUrl = photoUrl;
        this.phone = phone;
        this.role = role;
        // Set enabled automatically
        this.enabled = role.equalsIgnoreCase("client");
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public GeoPoint getAddress() { return address; }
    public void setAddress(GeoPoint address) { this.address = address; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) {
        this.role = role;
        this.enabled = role.equalsIgnoreCase("client"); // update enabled if role changes
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
