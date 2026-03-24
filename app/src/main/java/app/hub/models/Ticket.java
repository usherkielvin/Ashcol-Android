package app.hub.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ticket model class representing a service ticket document in Firestore.
 * Includes denormalized customer, branch, and employee data for efficient querying.
 */
public class Ticket {
    private String id;
    private String customerId;
    private String customerName; // Denormalized
    private String customerEmail; // Denormalized
    private String customerPhone; // Denormalized
    private String serviceType;
    private String description;
    private String status; // pending|assigned|in_progress|completed|cancelled
    private String priority; // low|medium|high
    private GeoPoint location;
    private String address;
    private String branchId;
    private String branchName; // Denormalized
    private String assignedEmployeeId;
    private String assignedEmployeeName; // Denormalized
    private Timestamp scheduledDate;
    private Timestamp completedDate;
    private List<String> photoUrls;
    private double estimatedCost;
    private double finalCost;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Default constructor required for Firestore
    public Ticket() {
        this.photoUrls = new ArrayList<>();
    }

    // Constructor with all fields
    public Ticket(String id, String customerId, String customerName, String customerEmail,
                  String customerPhone, String serviceType, String description, String status,
                  String priority, GeoPoint location, String address, String branchId,
                  String branchName, String assignedEmployeeId, String assignedEmployeeName,
                  Timestamp scheduledDate, Timestamp completedDate, List<String> photoUrls,
                  double estimatedCost, double finalCost, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.serviceType = serviceType;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.location = location;
        this.address = address;
        this.branchId = branchId;
        this.branchName = branchName;
        this.assignedEmployeeId = assignedEmployeeId;
        this.assignedEmployeeName = assignedEmployeeName;
        this.scheduledDate = scheduledDate;
        this.completedDate = completedDate;
        this.photoUrls = photoUrls != null ? photoUrls : new ArrayList<>();
        this.estimatedCost = estimatedCost;
        this.finalCost = finalCost;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
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

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getAssignedEmployeeId() {
        return assignedEmployeeId;
    }

    public void setAssignedEmployeeId(String assignedEmployeeId) {
        this.assignedEmployeeId = assignedEmployeeId;
    }

    public String getAssignedEmployeeName() {
        return assignedEmployeeName;
    }

    public void setAssignedEmployeeName(String assignedEmployeeName) {
        this.assignedEmployeeName = assignedEmployeeName;
    }

    public Timestamp getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(Timestamp scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public Timestamp getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Timestamp completedDate) {
        this.completedDate = completedDate;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls;
    }

    public double getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public double getFinalCost() {
        return finalCost;
    }

    public void setFinalCost(double finalCost) {
        this.finalCost = finalCost;
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
     * Converts the Ticket object to a Map for Firestore writes.
     * Excludes the @Exclude annotation to prevent serialization issues.
     * 
     * @return Map representation of the Ticket object
     */
    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("customerId", customerId);
        map.put("customerName", customerName);
        map.put("customerEmail", customerEmail);
        map.put("customerPhone", customerPhone);
        map.put("serviceType", serviceType);
        map.put("description", description);
        map.put("status", status);
        map.put("priority", priority);
        map.put("location", location);
        map.put("address", address);
        map.put("branchId", branchId);
        map.put("branchName", branchName);
        map.put("assignedEmployeeId", assignedEmployeeId);
        map.put("assignedEmployeeName", assignedEmployeeName);
        map.put("scheduledDate", scheduledDate);
        map.put("completedDate", completedDate);
        map.put("photoUrls", photoUrls);
        map.put("estimatedCost", estimatedCost);
        map.put("finalCost", finalCost);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }

    /**
     * Creates a Ticket object from a Firestore DocumentSnapshot.
     * 
     * @param snapshot Firestore document snapshot
     * @return Ticket object populated with data from the snapshot, or null if snapshot doesn't exist
     */
    public static Ticket fromSnapshot(DocumentSnapshot snapshot) {
        if (!snapshot.exists()) {
            return null;
        }

        Ticket ticket = new Ticket();
        ticket.setId(snapshot.getId());
        ticket.setCustomerId(snapshot.getString("customerId"));
        ticket.setCustomerName(snapshot.getString("customerName"));
        ticket.setCustomerEmail(snapshot.getString("customerEmail"));
        ticket.setCustomerPhone(snapshot.getString("customerPhone"));
        ticket.setServiceType(snapshot.getString("serviceType"));
        ticket.setDescription(snapshot.getString("description"));
        ticket.setStatus(snapshot.getString("status"));
        ticket.setPriority(snapshot.getString("priority"));
        ticket.setLocation(snapshot.getGeoPoint("location"));
        ticket.setAddress(snapshot.getString("address"));
        ticket.setBranchId(snapshot.getString("branchId"));
        ticket.setBranchName(snapshot.getString("branchName"));
        ticket.setAssignedEmployeeId(snapshot.getString("assignedEmployeeId"));
        ticket.setAssignedEmployeeName(snapshot.getString("assignedEmployeeName"));
        ticket.setScheduledDate(snapshot.getTimestamp("scheduledDate"));
        ticket.setCompletedDate(snapshot.getTimestamp("completedDate"));
        
        // Handle photoUrls list
        List<String> urls = (List<String>) snapshot.get("photoUrls");
        ticket.setPhotoUrls(urls != null ? urls : new ArrayList<>());
        
        // Handle numeric fields with null safety
        Double estimatedCost = snapshot.getDouble("estimatedCost");
        ticket.setEstimatedCost(estimatedCost != null ? estimatedCost : 0.0);
        
        Double finalCost = snapshot.getDouble("finalCost");
        ticket.setFinalCost(finalCost != null ? finalCost : 0.0);
        
        ticket.setCreatedAt(snapshot.getTimestamp("createdAt"));
        ticket.setUpdatedAt(snapshot.getTimestamp("updatedAt"));
        
        return ticket;
    }

    /**
     * Helper method to add a photo URL to the ticket's photo list.
     * 
     * @param photoUrl Photo URL to add
     */
    public void addPhotoUrl(String photoUrl) {
        if (photoUrls == null) {
            photoUrls = new ArrayList<>();
        }
        if (!photoUrls.contains(photoUrl)) {
            photoUrls.add(photoUrl);
        }
    }

    /**
     * Helper method to remove a photo URL from the ticket's photo list.
     * 
     * @param photoUrl Photo URL to remove
     */
    public void removePhotoUrl(String photoUrl) {
        if (photoUrls != null) {
            photoUrls.remove(photoUrl);
        }
    }

    /**
     * Helper method to check if ticket is pending.
     * 
     * @return true if ticket status is pending
     */
    @Exclude
    public boolean isPending() {
        return "pending".equals(status);
    }

    /**
     * Helper method to check if ticket is assigned.
     * 
     * @return true if ticket status is assigned
     */
    @Exclude
    public boolean isAssigned() {
        return "assigned".equals(status);
    }

    /**
     * Helper method to check if ticket is in progress.
     * 
     * @return true if ticket status is in_progress
     */
    @Exclude
    public boolean isInProgress() {
        return "in_progress".equals(status);
    }

    /**
     * Helper method to check if ticket is completed.
     * 
     * @return true if ticket status is completed
     */
    @Exclude
    public boolean isCompleted() {
        return "completed".equals(status);
    }

    /**
     * Helper method to check if ticket is cancelled.
     * 
     * @return true if ticket status is cancelled
     */
    @Exclude
    public boolean isCancelled() {
        return "cancelled".equals(status);
    }

    /**
     * Helper method to check if ticket has high priority.
     * 
     * @return true if ticket priority is high
     */
    @Exclude
    public boolean isHighPriority() {
        return "high".equals(priority);
    }

    /**
     * Helper method to check if ticket has medium priority.
     * 
     * @return true if ticket priority is medium
     */
    @Exclude
    public boolean isMediumPriority() {
        return "medium".equals(priority);
    }

    /**
     * Helper method to check if ticket has low priority.
     * 
     * @return true if ticket priority is low
     */
    @Exclude
    public boolean isLowPriority() {
        return "low".equals(priority);
    }

    @Override
    public String toString() {
        return "Ticket{" +
                "id='" + id + '\'' +
                ", customerId='" + customerId + '\'' +
                ", customerName='" + customerName + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", customerPhone='" + customerPhone + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", priority='" + priority + '\'' +
                ", location=" + location +
                ", address='" + address + '\'' +
                ", branchId='" + branchId + '\'' +
                ", branchName='" + branchName + '\'' +
                ", assignedEmployeeId='" + assignedEmployeeId + '\'' +
                ", assignedEmployeeName='" + assignedEmployeeName + '\'' +
                ", scheduledDate=" + scheduledDate +
                ", completedDate=" + completedDate +
                ", photoUrls=" + photoUrls +
                ", estimatedCost=" + estimatedCost +
                ", finalCost=" + finalCost +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
