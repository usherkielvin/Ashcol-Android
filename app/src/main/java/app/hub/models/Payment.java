package app.hub.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Payment model for Firestore subcollection under tickets
 * 
 * TODO: This is a placeholder class for integration tests.
 * Full implementation will be completed in Task 3.4.
 * 
 * Represents a payment document in the tickets/{ticketId}/payments subcollection
 */
public class Payment {
    
    private String id;
    private double amount;
    private String method; // cash, credit_card, digital_wallet
    private String status; // pending, paid, failed, refunded
    private String transactionId;
    private String employeeId;
    private Timestamp paidAt;
    private Timestamp createdAt;
    
    public Payment() {
        // Required empty constructor for Firestore
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    
    public Timestamp getPaidAt() {
        return paidAt;
    }
    
    public void setPaidAt(Timestamp paidAt) {
        this.paidAt = paidAt;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    // Firestore conversion methods
    
    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("amount", amount);
        map.put("method", method);
        map.put("status", status);
        map.put("transactionId", transactionId);
        map.put("employeeId", employeeId);
        map.put("paidAt", paidAt);
        map.put("createdAt", createdAt);
        return map;
    }
}
