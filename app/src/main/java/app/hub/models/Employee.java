package app.hub.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Employee model for Firestore subcollection under branches
 * 
 * TODO: This is a placeholder class for integration tests.
 * Full implementation will be completed in Task 3.x or 4.3.
 * 
 * Represents an employee document in the branches/{branchId}/employees subcollection
 */
public class Employee {
    
    private String userId;
    private String name;
    private String email;
    private String phone;
    private List<String> specializations;
    private boolean isAvailable;
    private int currentTicketCount;
    private int totalCompletedTickets;
    private double rating;
    private Timestamp joinedAt;
    private Timestamp updatedAt;
    
    public Employee() {
        // Required empty constructor for Firestore
    }
    
    // Getters and Setters
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public List<String> getSpecializations() {
        return specializations;
    }
    
    public void setSpecializations(List<String> specializations) {
        this.specializations = specializations;
    }
    
    public boolean isAvailable() {
        return isAvailable;
    }
    
    public void setAvailable(boolean available) {
        isAvailable = available;
    }
    
    public int getCurrentTicketCount() {
        return currentTicketCount;
    }
    
    public void setCurrentTicketCount(int currentTicketCount) {
        this.currentTicketCount = currentTicketCount;
    }
    
    public int getTotalCompletedTickets() {
        return totalCompletedTickets;
    }
    
    public void setTotalCompletedTickets(int totalCompletedTickets) {
        this.totalCompletedTickets = totalCompletedTickets;
    }
    
    public double getRating() {
        return rating;
    }
    
    public void setRating(double rating) {
        this.rating = rating;
    }
    
    public Timestamp getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
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
        map.put("userId", userId);
        map.put("name", name);
        map.put("email", email);
        map.put("phone", phone);
        map.put("specializations", specializations);
        map.put("isAvailable", isAvailable);
        map.put("currentTicketCount", currentTicketCount);
        map.put("totalCompletedTickets", totalCompletedTickets);
        map.put("rating", rating);
        map.put("joinedAt", joinedAt);
        map.put("updatedAt", updatedAt);
        return map;
    }
}
