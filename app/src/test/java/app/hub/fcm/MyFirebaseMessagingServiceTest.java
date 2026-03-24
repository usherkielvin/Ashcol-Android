package app.hub.fcm;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import com.google.firebase.messaging.RemoteMessage;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for MyFirebaseMessagingService (app.hub.services) and the
 * future Firebase-native MyFirebaseMessagingService (app.hub.fcm, task 11.2).
 *
 * **Validates: Requirement 19.4**
 *
 * Tests onMessageReceived handling, onNewToken token refresh,
 * and notification tap navigation to ticket detail screen.
 *
 * Tests for the task-11.2 implementation are annotated with @Ignore and will
 * be enabled once FCMManager (task 11.1) and the new service (task 11.2) are complete.
 */
@RunWith(RobolectricTestRunner.class)
public class MyFirebaseMessagingServiceTest {

    private Context context;

    // Existing service (app.hub.services) — available now
    private app.hub.services.MyFirebaseMessagingService existingService;

    // TODO: Replace with the new Firebase-native service once task 11.2 is implemented
    // private app.hub.fcm.MyFirebaseMessagingService service;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        // Existing service can be instantiated for structural tests
        existingService = new app.hub.services.MyFirebaseMessagingService();
        // TODO: Uncomment once new FCM service is implemented (task 11.2)
        // service = new app.hub.fcm.MyFirebaseMessagingService();
    }

    // ========== Existing Service - Structural Tests ==========

    @Test
    public void testExistingService_isNotNull() {
        assertNotNull("Existing MyFirebaseMessagingService should be instantiable", existingService);
    }

    @Test
    public void testExistingService_extendsFirebaseMessagingService() {
        assertTrue("Service should extend FirebaseMessagingService",
                existingService instanceof com.google.firebase.messaging.FirebaseMessagingService);
    }

    // ========== onMessageReceived Tests (task 11.2 implementation) ==========

    @Test
    @Ignore("TODO: Enable once new MyFirebaseMessagingService is implemented in task 11.2")
    public void testOnMessageReceived_withNotificationPayload_showsNotification() {
        // Arrange
        RemoteMessage message = buildRemoteMessage("ticket_001", "Ticket Assigned", "A technician is on the way");

        // TODO: Uncomment once new MyFirebaseMessagingService is implemented
        // service.onMessageReceived(message);
        // Verify a system notification was posted
    }

    @Test
    @Ignore("TODO: Enable once new MyFirebaseMessagingService is implemented in task 11.2")
    public void testOnMessageReceived_withDataPayload_extractsTicketId() {
        // Arrange - FCM data-only message (no notification block)
        Map<String, String> data = new HashMap<>();
        data.put("ticketId", "ticket_data_999");
        data.put("title", "Status Changed");
        data.put("body", "Your ticket is now in progress");
        RemoteMessage message = buildRemoteMessageWithData(data);

        // TODO: Uncomment once new MyFirebaseMessagingService is implemented
        // service.onMessageReceived(message);
        // Verify ticketId "ticket_data_999" is used in the notification PendingIntent
    }

    @Test
    @Ignore("TODO: Enable once new MyFirebaseMessagingService is implemented in task 11.2")
    public void testOnMessageReceived_withEmptyMessage_handlesGracefully() {
        // Arrange - message with no notification and no data
        RemoteMessage message = new RemoteMessage.Builder("test@fcm.googleapis.com").build();

        // TODO: Uncomment once new MyFirebaseMessagingService is implemented
        // service.onMessageReceived(message);
        // Verify no crash occurs and no notification is shown for empty messages
    }

    @Test
    @Ignore("TODO: Enable once new MyFirebaseMessagingService is implemented in task 11.2")
    public void testOnMessageReceived_withNullMessage_handlesGracefully() {
        // TODO: Uncomment once new MyFirebaseMessagingService is implemented
        // service.onMessageReceived(null);
        // Verify no NullPointerException is thrown
    }

    @Test
    @Ignore("TODO: Enable once new MyFirebaseMessagingService is implemented in task 11.2")
    public void testOnMessageReceived_ticketStatusChange_showsCorrectMessage() {
        // Arrange - status change notification
        Map<String, String> data = new HashMap<>();
        data.put("ticketId", "ticket_status_123");
        data.put("title", "Ticket Completed");
        data.put("body", "Your service ticket has been completed");
        data.put("status", "completed");
        RemoteMessage message = buildRemoteMessageWithData(data);

        // TODO: Uncomment once new MyFirebaseMessagingService is implemented
        // service.onMessageReceived(message);
        // Verify notification body reflects the status change
    }

    @Test
    @Ignore("TODO: Enable once new MyFirebaseMessagingService is implemented in task 11.2")
    public void testOnMessageReceived_employeeAssignment_showsAssignmentNotification() {
        // Arrange - employee assignment notification
        Map<String, String> data = new HashMap<>();
        data.put("ticketId", "ticket_assign_456");
        data.put("title", "New Ticket Assigned");
        data.put("body", "You have been assigned a new service ticket");
        RemoteMessage message = buildRemoteMessageWithData(data);

        // TODO: Uncomment once new MyFirebaseMessagingService is implemented
        // service.onMessageReceived(message);
        // Verify notification is shown with correct assignment details
    }

    // ========== onNewToken Tests ==========

    @Test
    @Ignore("TODO: Enable once new MyFirebaseMessagingService is implemented in task 11.2")
    public void testOnNewToken_withValidToken_updatesFirestore() {
        // Arrange
        String newToken = "new_fcm_token_xyz_789";

        // TODO: Uncomment once new MyFirebaseMessagingService is implemented
        // service.onNewToken(newToken);
        // Verify the token is saved to the current user's Firestore document
    }

    @Test
    @Ignore("TODO: Enable once new MyFirebaseMessagingService is implemented in task 11.2")
    public void testOnNewToken_whenUserNotSignedIn_doesNotCrash() {
        // Arrange - no authenticated user
        String newToken = "token_no_user_abc";

        // TODO: Uncomment once new MyFirebaseMessagingService is implemented
        // service.onNewToken(newToken);
        // Verify no crash occurs when there is no signed-in user
    }

    @Test
    @Ignore("TODO: Enable once new MyFirebaseMessagingService is implemented in task 11.2")
    public void testOnNewToken_withNullToken_handlesGracefully() {
        // TODO: Uncomment once new MyFirebaseMessagingService is implemented
        // service.onNewToken(null);
        // Verify no NullPointerException is thrown
    }

    @Test
    @Ignore("TODO: Enable once new MyFirebaseMessagingService is implemented in task 11.2")
    public void testOnNewToken_withEmptyToken_handlesGracefully() {
        // TODO: Uncomment once new MyFirebaseMessagingService is implemented
        // service.onNewToken("");
        // Verify empty token is not saved to Firestore
    }

    @Test
    @Ignore("TODO: Enable once new MyFirebaseMessagingService is implemented in task 11.2")
    public void testOnNewToken_replacesOldToken_inFirestore() {
        // Arrange - simulate token rotation (old token replaced by new one)
        String oldToken = "old_fcm_token_111";
        String newToken = "new_fcm_token_222";

        // TODO: Uncomment once new MyFirebaseMessagingService is implemented
        // service.onNewToken(oldToken);
        // service.onNewToken(newToken);
        // Verify Firestore contains newToken and old token handling is correct
    }

    // ========== Notification Navigation Tests ==========

    @Test
    @Ignore("TODO: Enable once new MyFirebaseMessagingService is implemented in task 11.2")
    public void testNotificationTap_withTicketId_opensTicketDetailScreen() {
        // Arrange - notification with ticketId in payload
        String ticketId = "ticket_nav_001";
        RemoteMessage message = buildRemoteMessage(ticketId, "Ticket Update", "Your ticket was updated");

        // TODO: Uncomment once new MyFirebaseMessagingService is implemented
        // service.onMessageReceived(message);
        // Retrieve the PendingIntent from the posted notification
        // Verify the Intent targets the ticket detail Activity
        // Verify the Intent contains EXTRA_TICKET_ID = "ticket_nav_001"
    }

    @Test
    @Ignore("TODO: Enable once new MyFirebaseMessagingService is implemented in task 11.2")
    public void testNotificationTap_withoutTicketId_opensMainScreen() {
        // Arrange - notification without ticketId
        RemoteMessage message = buildRemoteMessage(null, "General Update", "App has been updated");

        // TODO: Uncomment once new MyFirebaseMessagingService is implemented
        // service.onMessageReceived(message);
        // Verify the PendingIntent targets the main/home Activity (not ticket detail)
    }

    @Test
    @Ignore("TODO: Enable once new MyFirebaseMessagingService is implemented in task 11.2")
    public void testNotificationIntent_containsCorrectTicketId() {
        // Arrange
        String expectedTicketId = "ticket_intent_check_789";
        Map<String, String> data = new HashMap<>();
        data.put("ticketId", expectedTicketId);
        data.put("title", "Ticket Ready");
        data.put("body", "Your ticket is ready for pickup");
        RemoteMessage message = buildRemoteMessageWithData(data);

        // TODO: Uncomment once new MyFirebaseMessagingService is implemented
        // service.onMessageReceived(message);
        // Capture the Intent from the notification's PendingIntent
        // assertEquals(expectedTicketId, intent.getStringExtra("ticketId"));
    }

    @Test
    @Ignore("TODO: Enable once new MyFirebaseMessagingService is implemented in task 11.2")
    public void testNotificationIntent_usesCorrectFlags() {
        // Verify the notification Intent uses FLAG_ACTIVITY_CLEAR_TOP or similar
        // to avoid stacking multiple ticket detail activities
        RemoteMessage message = buildRemoteMessage("ticket_flags_001", "Update", "Status changed");

        // TODO: Uncomment once new MyFirebaseMessagingService is implemented
        // service.onMessageReceived(message);
        // Verify Intent flags include FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP
    }

    // ========== Notification Channel Tests ==========

    @Test
    @Ignore("TODO: Enable once new MyFirebaseMessagingService is implemented in task 11.2")
    public void testNotificationChannel_isCreatedOnFirstNotification() {
        // Android O+ requires a notification channel before posting
        RemoteMessage message = buildRemoteMessage("ticket_ch_001", "Title", "Body");

        // TODO: Uncomment once new MyFirebaseMessagingService is implemented
        // service.onMessageReceived(message);
        // NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // assertNotNull("Notification channel should exist", nm.getNotificationChannel("fcm_channel"));
    }

    // ========== Helper Methods ==========

    /**
     * Builds a RemoteMessage with data payload entries for ticketId, title, and body.
     */
    private RemoteMessage buildRemoteMessage(String ticketId, String title, String body) {
        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@fcm.googleapis.com");
        if (ticketId != null) {
            builder.addData("ticketId", ticketId);
        }
        if (title != null) {
            builder.addData("title", title);
        }
        if (body != null) {
            builder.addData("body", body);
        }
        return builder.build();
    }

    /**
     * Builds a RemoteMessage from an arbitrary data map.
     */
    private RemoteMessage buildRemoteMessageWithData(Map<String, String> data) {
        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@fcm.googleapis.com");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            builder.addData(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }
}
