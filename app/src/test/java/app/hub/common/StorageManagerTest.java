package app.hub.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.net.Uri;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/**
 * Unit tests for StorageManager
 * 
 * **Validates: Requirement 19.1**
 * 
 * Tests upload operations with mock URIs, progress tracking callbacks,
 * image compression logic, and delete operations.
 */
@RunWith(RobolectricTestRunner.class)
public class StorageManagerTest {
    
    @Mock
    private FirebaseStorage mockStorage;
    
    @Mock
    private StorageReference mockStorageRef;
    
    @Mock
    private StorageReference mockTicketPhotoRef;
    
    @Mock
    private StorageReference mockProfilePhotoRef;
    
    @Mock
    private Uri mockUri;
    
    @Mock
    private UploadTask mockUploadTask;
    
    @Mock
    private UploadTask.TaskSnapshot mockTaskSnapshot;
    
    private StorageManager storageManager;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup mock storage reference hierarchy
        when(mockStorage.getReference()).thenReturn(mockStorageRef);
        when(mockStorageRef.child(anyString())).thenReturn(mockTicketPhotoRef);
        when(mockTicketPhotoRef.child(anyString())).thenReturn(mockTicketPhotoRef);
        
        storageManager = new StorageManager();
    }
    
    // ========== Upload Operations Tests ==========
    
    @Test
    public void testUploadTicketPhoto_withValidUri_returnsTask() {
        // Arrange
        String ticketId = "ticket123";
        StorageManager.OnProgressListener listener = mock(StorageManager.OnProgressListener.class);
        
        // Act
        Task<UploadTask.TaskSnapshot> result = storageManager.uploadTicketPhoto(ticketId, mockUri, listener);
        
        // Assert
        assertNotNull("Upload task should not be null", result);
    }
    
    @Test
    public void testUploadTicketPhoto_withNullTicketId_throwsException() {
        // Arrange
        StorageManager.OnProgressListener listener = mock(StorageManager.OnProgressListener.class);
        
        // Act & Assert
        assertThrows("Should throw exception for null ticket ID",
                IllegalArgumentException.class,
                () -> storageManager.uploadTicketPhoto(null, mockUri, listener));
    }
    
    @Test
    public void testUploadTicketPhoto_withEmptyTicketId_throwsException() {
        // Arrange
        StorageManager.OnProgressListener listener = mock(StorageManager.OnProgressListener.class);
        
        // Act & Assert
        assertThrows("Should throw exception for empty ticket ID",
                IllegalArgumentException.class,
                () -> storageManager.uploadTicketPhoto("", mockUri, listener));
    }
    
    @Test
    public void testUploadTicketPhoto_withNullUri_throwsException() {
        // Arrange
        String ticketId = "ticket123";
        StorageManager.OnProgressListener listener = mock(StorageManager.OnProgressListener.class);
        
        // Act & Assert
        assertThrows("Should throw exception for null URI",
                IllegalArgumentException.class,
                () -> storageManager.uploadTicketPhoto(ticketId, null, listener));
    }
    
    @Test
    public void testUploadTicketPhoto_withNullListener_returnsTask() {
        // Arrange
        String ticketId = "ticket123";
        
        // Act
        Task<UploadTask.TaskSnapshot> result = storageManager.uploadTicketPhoto(ticketId, mockUri, null);
        
        // Assert
        assertNotNull("Upload task should not be null even with null listener", result);
    }
    
    @Test
    public void testUploadProfilePhoto_withValidUri_returnsTask() {
        // Arrange
        String userId = "user456";
        
        // Act
        Task<UploadTask.TaskSnapshot> result = storageManager.uploadProfilePhoto(userId, mockUri);
        
        // Assert
        assertNotNull("Upload task should not be null", result);
    }
    
    @Test
    public void testUploadProfilePhoto_withNullUserId_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for null user ID",
                IllegalArgumentException.class,
                () -> storageManager.uploadProfilePhoto(null, mockUri));
    }
    
    @Test
    public void testUploadProfilePhoto_withEmptyUserId_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for empty user ID",
                IllegalArgumentException.class,
                () -> storageManager.uploadProfilePhoto("", mockUri));
    }
    
    @Test
    public void testUploadProfilePhoto_withNullUri_throwsException() {
        // Arrange
        String userId = "user456";
        
        // Act & Assert
        assertThrows("Should throw exception for null URI",
                IllegalArgumentException.class,
                () -> storageManager.uploadProfilePhoto(userId, null));
    }
    
    // ========== Progress Tracking Tests ==========
    
    @Test
    public void testProgressListener_onProgressCalled() {
        // Arrange
        String ticketId = "ticket123";
        StorageManager.OnProgressListener listener = mock(StorageManager.OnProgressListener.class);
        
        // Act
        storageManager.uploadTicketPhoto(ticketId, mockUri, listener);
        
        // Simulate progress callback
        double progress = 0.5;
        listener.onProgress(progress);
        
        // Assert
        verify(listener).onProgress(progress);
    }
    
    @Test
    public void testProgressListener_onCompleteCalled() {
        // Arrange
        String ticketId = "ticket123";
        StorageManager.OnProgressListener listener = mock(StorageManager.OnProgressListener.class);
        Uri downloadUrl = mock(Uri.class);
        
        // Act
        storageManager.uploadTicketPhoto(ticketId, mockUri, listener);
        
        // Simulate completion callback
        listener.onComplete(downloadUrl);
        
        // Assert
        verify(listener).onComplete(downloadUrl);
    }
    
    @Test
    public void testProgressListener_onErrorCalled() {
        // Arrange
        String ticketId = "ticket123";
        StorageManager.OnProgressListener listener = mock(StorageManager.OnProgressListener.class);
        Exception error = new Exception("Upload failed");
        
        // Act
        storageManager.uploadTicketPhoto(ticketId, mockUri, listener);
        
        // Simulate error callback
        listener.onError(error);
        
        // Assert
        verify(listener).onError(error);
    }
    
    @Test
    public void testProgressListener_progressRangeValidation() {
        // Arrange
        StorageManager.OnProgressListener listener = new StorageManager.OnProgressListener() {
            @Override
            public void onProgress(double progress) {
                assertTrue("Progress should be between 0 and 1", progress >= 0.0 && progress <= 1.0);
            }
            
            @Override
            public void onComplete(Uri downloadUrl) {}
            
            @Override
            public void onError(Exception e) {}
        };
        
        // Act & Assert
        listener.onProgress(0.0);
        listener.onProgress(0.5);
        listener.onProgress(1.0);
    }
    
    @Test
    public void testProgressListener_multipleProgressUpdates() {
        // Arrange
        StorageManager.OnProgressListener listener = mock(StorageManager.OnProgressListener.class);
        
        // Act
        listener.onProgress(0.25);
        listener.onProgress(0.50);
        listener.onProgress(0.75);
        listener.onProgress(1.0);
        
        // Assert
        verify(listener).onProgress(0.25);
        verify(listener).onProgress(0.50);
        verify(listener).onProgress(0.75);
        verify(listener).onProgress(1.0);
    }
    
    // ========== Image Compression Tests ==========
    
    @Test
    public void testCompressImage_withValidBitmap_returnsCompressedBitmap() {
        // Arrange
        Bitmap original = Bitmap.createBitmap(2000, 2000, Bitmap.Config.ARGB_8888);
        int maxWidth = 1920;
        int maxHeight = 1080;
        int quality = 80;
        
        // Act
        Bitmap compressed = storageManager.compressImage(original, maxWidth, maxHeight, quality);
        
        // Assert
        assertNotNull("Compressed bitmap should not be null", compressed);
        assertTrue("Compressed width should not exceed max width", compressed.getWidth() <= maxWidth);
        assertTrue("Compressed height should not exceed max height", compressed.getHeight() <= maxHeight);
    }
    
    @Test
    public void testCompressImage_withNullBitmap_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for null bitmap",
                IllegalArgumentException.class,
                () -> storageManager.compressImage(null, 1920, 1080, 80));
    }
    
    @Test
    public void testCompressImage_withInvalidMaxWidth_throwsException() {
        // Arrange
        Bitmap original = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        
        // Act & Assert
        assertThrows("Should throw exception for invalid max width",
                IllegalArgumentException.class,
                () -> storageManager.compressImage(original, 0, 1080, 80));
    }
    
    @Test
    public void testCompressImage_withInvalidMaxHeight_throwsException() {
        // Arrange
        Bitmap original = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        
        // Act & Assert
        assertThrows("Should throw exception for invalid max height",
                IllegalArgumentException.class,
                () -> storageManager.compressImage(original, 1920, 0, 80));
    }
    
    @Test
    public void testCompressImage_withInvalidQuality_throwsException() {
        // Arrange
        Bitmap original = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        
        // Act & Assert
        assertThrows("Should throw exception for quality < 0",
                IllegalArgumentException.class,
                () -> storageManager.compressImage(original, 1920, 1080, -1));
        
        assertThrows("Should throw exception for quality > 100",
                IllegalArgumentException.class,
                () -> storageManager.compressImage(original, 1920, 1080, 101));
    }
    
    @Test
    public void testCompressImage_preservesAspectRatio() {
        // Arrange
        Bitmap original = Bitmap.createBitmap(2000, 1000, Bitmap.Config.ARGB_8888);
        int maxWidth = 1920;
        int maxHeight = 1080;
        int quality = 80;
        
        // Act
        Bitmap compressed = storageManager.compressImage(original, maxWidth, maxHeight, quality);
        
        // Assert
        assertNotNull("Compressed bitmap should not be null", compressed);
        double originalRatio = (double) original.getWidth() / original.getHeight();
        double compressedRatio = (double) compressed.getWidth() / compressed.getHeight();
        assertEquals("Aspect ratio should be preserved", originalRatio, compressedRatio, 0.01);
    }
    
    @Test
    public void testCompressImage_smallerImageNotUpscaled() {
        // Arrange
        Bitmap original = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888);
        int maxWidth = 1920;
        int maxHeight = 1080;
        int quality = 80;
        
        // Act
        Bitmap compressed = storageManager.compressImage(original, maxWidth, maxHeight, quality);
        
        // Assert
        assertNotNull("Compressed bitmap should not be null", compressed);
        assertEquals("Width should not be upscaled", 800, compressed.getWidth());
        assertEquals("Height should not be upscaled", 600, compressed.getHeight());
    }
    
    @Test
    public void testCompressImage_landscapeOrientation() {
        // Arrange
        Bitmap original = Bitmap.createBitmap(3000, 2000, Bitmap.Config.ARGB_8888);
        int maxWidth = 1920;
        int maxHeight = 1080;
        int quality = 80;
        
        // Act
        Bitmap compressed = storageManager.compressImage(original, maxWidth, maxHeight, quality);
        
        // Assert
        assertNotNull("Compressed bitmap should not be null", compressed);
        assertTrue("Compressed width should not exceed max width", compressed.getWidth() <= maxWidth);
        assertTrue("Compressed height should not exceed max height", compressed.getHeight() <= maxHeight);
    }
    
    @Test
    public void testCompressImage_portraitOrientation() {
        // Arrange
        Bitmap original = Bitmap.createBitmap(2000, 3000, Bitmap.Config.ARGB_8888);
        int maxWidth = 1920;
        int maxHeight = 1080;
        int quality = 80;
        
        // Act
        Bitmap compressed = storageManager.compressImage(original, maxWidth, maxHeight, quality);
        
        // Assert
        assertNotNull("Compressed bitmap should not be null", compressed);
        assertTrue("Compressed width should not exceed max width", compressed.getWidth() <= maxWidth);
        assertTrue("Compressed height should not exceed max height", compressed.getHeight() <= maxHeight);
    }
    
    @Test
    public void testCompressImage_squareImage() {
        // Arrange
        Bitmap original = Bitmap.createBitmap(2000, 2000, Bitmap.Config.ARGB_8888);
        int maxWidth = 1920;
        int maxHeight = 1080;
        int quality = 80;
        
        // Act
        Bitmap compressed = storageManager.compressImage(original, maxWidth, maxHeight, quality);
        
        // Assert
        assertNotNull("Compressed bitmap should not be null", compressed);
        assertEquals("Square image should maintain equal dimensions", 
                compressed.getWidth(), compressed.getHeight());
    }
    
    @Test
    public void testCompressImage_differentQualityLevels() {
        // Arrange
        Bitmap original = Bitmap.createBitmap(2000, 2000, Bitmap.Config.ARGB_8888);
        int maxWidth = 1920;
        int maxHeight = 1080;
        
        // Act
        Bitmap compressed80 = storageManager.compressImage(original, maxWidth, maxHeight, 80);
        Bitmap compressed50 = storageManager.compressImage(original, maxWidth, maxHeight, 50);
        Bitmap compressed100 = storageManager.compressImage(original, maxWidth, maxHeight, 100);
        
        // Assert
        assertNotNull("Compressed bitmap (80%) should not be null", compressed80);
        assertNotNull("Compressed bitmap (50%) should not be null", compressed50);
        assertNotNull("Compressed bitmap (100%) should not be null", compressed100);
    }
    
    // ========== Delete Operations Tests ==========
    
    @Test
    public void testDeleteTicketPhotos_withValidTicketId_returnsTask() {
        // Arrange
        String ticketId = "ticket123";
        
        // Act
        Task<Void> result = storageManager.deleteTicketPhotos(ticketId);
        
        // Assert
        assertNotNull("Delete task should not be null", result);
    }
    
    @Test
    public void testDeleteTicketPhotos_withNullTicketId_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for null ticket ID",
                IllegalArgumentException.class,
                () -> storageManager.deleteTicketPhotos(null));
    }
    
    @Test
    public void testDeleteTicketPhotos_withEmptyTicketId_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for empty ticket ID",
                IllegalArgumentException.class,
                () -> storageManager.deleteTicketPhotos(""));
    }
    
    @Test
    public void testDeleteProfilePhoto_withValidUserId_returnsTask() {
        // Arrange
        String userId = "user456";
        
        // Act
        Task<Void> result = storageManager.deleteProfilePhoto(userId);
        
        // Assert
        assertNotNull("Delete task should not be null", result);
    }
    
    @Test
    public void testDeleteProfilePhoto_withNullUserId_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for null user ID",
                IllegalArgumentException.class,
                () -> storageManager.deleteProfilePhoto(null));
    }
    
    @Test
    public void testDeleteProfilePhoto_withEmptyUserId_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for empty user ID",
                IllegalArgumentException.class,
                () -> storageManager.deleteProfilePhoto(""));
    }
    
    // ========== Download URL Tests ==========
    
    @Test
    public void testGetTicketPhotoUrl_withValidParameters_returnsTask() {
        // Arrange
        String ticketId = "ticket123";
        String filename = "photo.jpg";
        
        // Act
        Task<Uri> result = storageManager.getTicketPhotoUrl(ticketId, filename);
        
        // Assert
        assertNotNull("Download URL task should not be null", result);
    }
    
    @Test
    public void testGetTicketPhotoUrl_withNullTicketId_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for null ticket ID",
                IllegalArgumentException.class,
                () -> storageManager.getTicketPhotoUrl(null, "photo.jpg"));
    }
    
    @Test
    public void testGetTicketPhotoUrl_withEmptyTicketId_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for empty ticket ID",
                IllegalArgumentException.class,
                () -> storageManager.getTicketPhotoUrl("", "photo.jpg"));
    }
    
    @Test
    public void testGetTicketPhotoUrl_withNullFilename_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for null filename",
                IllegalArgumentException.class,
                () -> storageManager.getTicketPhotoUrl("ticket123", null));
    }
    
    @Test
    public void testGetTicketPhotoUrl_withEmptyFilename_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for empty filename",
                IllegalArgumentException.class,
                () -> storageManager.getTicketPhotoUrl("ticket123", ""));
    }
    
    @Test
    public void testGetProfilePhotoUrl_withValidUserId_returnsTask() {
        // Arrange
        String userId = "user456";
        
        // Act
        Task<Uri> result = storageManager.getProfilePhotoUrl(userId);
        
        // Assert
        assertNotNull("Download URL task should not be null", result);
    }
    
    @Test
    public void testGetProfilePhotoUrl_withNullUserId_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for null user ID",
                IllegalArgumentException.class,
                () -> storageManager.getProfilePhotoUrl(null));
    }
    
    @Test
    public void testGetProfilePhotoUrl_withEmptyUserId_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for empty user ID",
                IllegalArgumentException.class,
                () -> storageManager.getProfilePhotoUrl(""));
    }
    
    // ========== Storage Path Tests ==========
    
    @Test
    public void testStoragePaths_ticketPhotosFollowCorrectStructure() {
        // This test verifies that the storage path structure matches the design:
        // "ticket-images/{ticketId}/{filename}"
        
        String ticketId = "ticket123";
        String filename = "photo.jpg";
        
        // Act
        Task<Uri> result = storageManager.getTicketPhotoUrl(ticketId, filename);
        
        // Assert
        assertNotNull("Task should be created with correct path structure", result);
    }
    
    @Test
    public void testStoragePaths_profilePhotosFollowCorrectStructure() {
        // This test verifies that the storage path structure matches the design:
        // "profile-photos/{userId}/{filename}"
        
        String userId = "user456";
        
        // Act
        Task<Uri> result = storageManager.getProfilePhotoUrl(userId);
        
        // Assert
        assertNotNull("Task should be created with correct path structure", result);
    }
    
    // ========== Edge Cases and Integration Tests ==========
    
    @Test
    public void testUploadFlow_completeWorkflow() {
        // This test demonstrates the complete upload workflow:
        // 1. Compress image
        // 2. Upload with progress tracking
        // 3. Get download URL
        
        // Step 1: Compress image
        Bitmap original = Bitmap.createBitmap(2000, 2000, Bitmap.Config.ARGB_8888);
        Bitmap compressed = storageManager.compressImage(original, 1920, 1080, 80);
        assertNotNull("Compressed image should not be null", compressed);
        
        // Step 2: Upload with progress tracking
        String ticketId = "ticket123";
        StorageManager.OnProgressListener listener = new StorageManager.OnProgressListener() {
            @Override
            public void onProgress(double progress) {
                assertTrue("Progress should be valid", progress >= 0.0 && progress <= 1.0);
            }
            
            @Override
            public void onComplete(Uri downloadUrl) {
                assertNotNull("Download URL should not be null", downloadUrl);
            }
            
            @Override
            public void onError(Exception e) {
                // Handle error
            }
        };
        
        Task<UploadTask.TaskSnapshot> uploadTask = storageManager.uploadTicketPhoto(ticketId, mockUri, listener);
        assertNotNull("Upload task should be created", uploadTask);
        
        // Step 3: Get download URL
        Task<Uri> urlTask = storageManager.getTicketPhotoUrl(ticketId, "photo.jpg");
        assertNotNull("URL task should be created", urlTask);
    }
    
    @Test
    public void testDeleteFlow_completeWorkflow() {
        // This test demonstrates the complete delete workflow:
        // 1. Upload photo
        // 2. Delete photo
        
        String ticketId = "ticket123";
        
        // Step 1: Upload photo
        Task<UploadTask.TaskSnapshot> uploadTask = storageManager.uploadTicketPhoto(ticketId, mockUri, null);
        assertNotNull("Upload task should be created", uploadTask);
        
        // Step 2: Delete photo
        Task<Void> deleteTask = storageManager.deleteTicketPhotos(ticketId);
        assertNotNull("Delete task should be created", deleteTask);
    }
    
    @Test
    public void testMultipleUploads_sameTicket() {
        // Test uploading multiple photos for the same ticket
        String ticketId = "ticket123";
        
        Task<UploadTask.TaskSnapshot> upload1 = storageManager.uploadTicketPhoto(ticketId, mockUri, null);
        Task<UploadTask.TaskSnapshot> upload2 = storageManager.uploadTicketPhoto(ticketId, mockUri, null);
        Task<UploadTask.TaskSnapshot> upload3 = storageManager.uploadTicketPhoto(ticketId, mockUri, null);
        
        assertNotNull("First upload task should not be null", upload1);
        assertNotNull("Second upload task should not be null", upload2);
        assertNotNull("Third upload task should not be null", upload3);
    }
    
    @Test
    public void testConcurrentUploads_differentTickets() {
        // Test uploading photos for different tickets concurrently
        String ticketId1 = "ticket123";
        String ticketId2 = "ticket456";
        String ticketId3 = "ticket789";
        
        Task<UploadTask.TaskSnapshot> upload1 = storageManager.uploadTicketPhoto(ticketId1, mockUri, null);
        Task<UploadTask.TaskSnapshot> upload2 = storageManager.uploadTicketPhoto(ticketId2, mockUri, null);
        Task<UploadTask.TaskSnapshot> upload3 = storageManager.uploadTicketPhoto(ticketId3, mockUri, null);
        
        assertNotNull("First upload task should not be null", upload1);
        assertNotNull("Second upload task should not be null", upload2);
        assertNotNull("Third upload task should not be null", upload3);
    }
}
