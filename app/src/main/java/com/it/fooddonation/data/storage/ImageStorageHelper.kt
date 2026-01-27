package com.it.fooddonation.data.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import com.it.fooddonation.data.remote.SupabaseClient
import io.github.jan.supabase.storage.storage
import java.util.UUID

/**
 * Helper class for uploading images to Supabase Storage
 */
class ImageStorageHelper(private val context: Context) {

    private val supabase = SupabaseClient.client
    private val bucketName = "donation-images"

    companion object {
        private const val TAG = "ImageStorageHelper"
    }

    /**
     * Upload an image to Supabase Storage
     * @param imageUri Local URI of the image
     * @param donorId ID of the donor (for organizing images)
     * @return Public URL of the uploaded image, or null if upload fails
     */
    suspend fun uploadDonationImage(imageUri: Uri, donorId: String): Result<String> {
        return try {
            Log.d(TAG, "uploadDonationImage: Starting upload for URI: $imageUri")

            // Read image bytes from URI
            val imageBytes = context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: run {
                Log.e(TAG, "uploadDonationImage: Failed to read image bytes")
                return Result.failure(Exception("Failed to read image file"))
            }

            Log.d(TAG, "uploadDonationImage: Image size: ${imageBytes.size} bytes")

            // Generate unique filename
            val fileName = "${donorId}/${UUID.randomUUID()}.jpg"
            Log.d(TAG, "uploadDonationImage: Uploading as: $fileName")

            // Upload to Supabase Storage
            supabase.storage[bucketName].upload(
                path = fileName,
                data = imageBytes
            ) {
                upsert = false
            }

            // Get public URL
            val publicUrl = supabase.storage[bucketName].publicUrl(fileName)
            Log.d(TAG, "uploadDonationImage: Upload successful. Public URL: $publicUrl")

            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e(TAG, "uploadDonationImage: Upload failed", e)
            Result.failure(e)
        }
    }

    /**
     * Delete an image from Supabase Storage
     * @param imageUrl Public URL of the image to delete
     */
    suspend fun deleteDonationImage(imageUrl: String): Result<Unit> {
        return try {
            // Extract filename from URL
            val fileName = imageUrl.substringAfter("$bucketName/")
            Log.d(TAG, "deleteDonationImage: Deleting file: $fileName")

            supabase.storage[bucketName].delete(fileName)

            Log.d(TAG, "deleteDonationImage: Delete successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteDonationImage: Delete failed", e)
            Result.failure(e)
        }
    }
}
