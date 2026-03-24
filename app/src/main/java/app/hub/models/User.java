package app.hub.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User model class representing a user document in Firestore.
 * Supports four user roles: admin, manager, employee, and user.
 */
public class User {
    private String uid;
    private String email;
    private String name;
    private String phone;
    private String role; // admin|manager|employee|user
    private String branchId; // for manager/employee roles
    private List<String> fcmTokens;
    private String profilePhotoUrl;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Default constructor required for Firestore
    public User() {
        this.fcmTokens = new ArrayList<>();
    }

    // Constructor with all fields
    public User(String uid, String email, String name, String phone, String role, 
                String branchId, List<String> fcmTokens, String profilePhotoUrl, 
                Timestamp createdAt, Timestamp updatedAt) {
        this.uid = uid;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.branchId = branchId;
        this.fcmTokens = fcmTokens != null ? fcmTokens : new ArrayList<>();
        this.profilePhotoUrl = profilePhotoUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public List<String> getFcmTokens() {
        return fcmTokens;
    }

    public void setFcmTokens(List<String> fcmTokens) {
        this.fcmTokens = fcmTokens;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
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

    /**
     * Converts the User object to a Map for Firestore writes.
     * Excludes the @Exclude annotation to prevent serialization issues.
     * 
     * @return Map representation of the User object
     */
    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("email", email);
        map.put("name", name);
        map.put("phone", phone);
        map.put("role", role);
        map.put("branchId", branchId);
        map.put("fcmTokens", fcmTokens);
        map.put("profilePhotoUrl", profilePhotoUrl);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }

    /**
     * Creates a User object from a Firestore DocumentSnapshot.
     * 
     * @param snapshot Firestore document snapshot
     * @return User object populated with data from the snapshot, or null if snapshot doesn't exist
     */
    public static User fromSnapshot(DocumentSnapshot snapshot) {
        if (!snapshot.exists()) {
            return null;
        }

        User user = new User();
        user.setUid(snapshot.getString("uid"));
        user.setEmail(snapshot.getString("email"));
        user.setName(snapshot.getString("name"));
        user.setPhone(snapshot.getString("phone"));
        user.setRole(snapshot.getString("role"));
        user.setBranchId(snapshot.getString("branchId"));
        
        // Handle fcmTokens list
        List<String> tokens = (List<String>) snapshot.get("fcmTokens");
        user.setFcmTokens(tokens != null ? tokens : new ArrayList<>());
        
        user.setProfilePhotoUrl(snapshot.getString("profilePhotoUrl"));
        user.setCreatedAt(snapshot.getTimestamp("createdAt"));
        user.setUpdatedAt(snapshot.getTimestamp("updatedAt"));
        
        return user;
    }

    /**
     * Helper method to check if user is an admin.
     * 
     * @return true if user role is admin
     */
    @Exclude
    public boolean isAdmin() {
        return "admin".equals(role);
    }

    /**
     * Helper method to check if user is a manager.
     * 
     * @return true if user role is manager
     */
    @Exclude
    public boolean isManager() {
        return "manager".equals(role);
    }

    /**
     * Helper method to check if user is an employee.
     * 
     * @return true if user role is employee
     */
    @Exclude
    public boolean isEmployee() {
        return "employee".equals(role);
    }

    /**
     * Helper method to check if user is a regular user.
     * 
     * @return true if user role is user
     */
    @Exclude
    public boolean isRegularUser() {
        return "user".equals(role);
    }

    /**
     * Helper method to add an FCM token to the user's token list.
     * 
     * @param token FCM token to add
     */
    public void addFcmToken(String token) {
        if (fcmTokens == null) {
            fcmTokens = new ArrayList<>();
        }
        if (!fcmTokens.contains(token)) {
            fcmTokens.add(token);
        }
    }

    /**
     * Helper method to remove an FCM token from the user's token list.
     * 
     * @param token FCM token to remove
     */
    public void removeFcmToken(String token) {
        if (fcmTokens != null) {
            fcmTokens.remove(token);
        }
    }

    @Override
    public String toString() {
        return "User{" +
                "uid='" + uid + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", role='" + role + '\'' +
                ", branchId='" + branchId + '\'' +
                ", fcmTokens=" + fcmTokens +
                ", profilePhotoUrl='" + profilePhotoUrl + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
