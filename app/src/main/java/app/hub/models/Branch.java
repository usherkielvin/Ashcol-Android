package app.hub.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

/**
 * Branch model for Firestore
 * 
 * TODO: This is a placeholder class for integration tests.
 * Full implementation will be completed in Task 3.3.
 * 
 * Represents a branch document in the branches collection
 */
public class Branch {
    
    private String id;
    private String name;
    private GeoPoint location;
    private String address;
    private double coverageRadius;
    private String managerId;
    private String managerName;
    private String phone;
    private String email;
    private boolean isActive;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    public Branch() {
        // Required empty constructor for Firestore
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
    
    public GeoPoint getLocation() {
        return location;
    }
    
    public void setLocation(GeoPoint location) {
        this.location = location;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public double getCoverageRadius() {
        return coverageRadius;
    }
    
    public void setCoverageRadius(double coverageRadius) {
        this.coverageRadius = coverageRadius;
    }
    
    public String getManagerId() {
        return managerId;
    }
    
    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }
    
    public String getManagerName() {
        return managerName;
    }
    
    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Firestore conversion methods
    
    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("location", location);
        map.put("address", address);
        map.put("coverageRadius", coverageRadius);
        map.put("managerId", managerId);
        map.put("managerName", managerName);
        map.put("phone", phone);
        map.put("email", email);
        map.put("isActive", isActive);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }
}
