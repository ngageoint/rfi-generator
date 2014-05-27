package models;

// Simple enumeration around the types of activities available for tracking
public enum ActivityType {
    CREATED(1), STARTED(2), COMPLETED(3), COMMENTED(4), CONTENT_UPDATED(5), VERIFIED(6), ASSIGNED(7), UPLOAD_ADDED(8), CANCELED(9), PERSISTENT(10), PRODUCT_ADDED(11), WEBLINK_ADDED(12), ON_HOLD(13), WITH_CUSTOMER(14);
    
    private int value;
    
    private ActivityType(int value) {
        this.setValue(value);
    }
    
    public void setValue(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public String toString() {
        return this.name();
    }
}
