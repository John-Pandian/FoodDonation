package com.it.fooddonation.data.repository

import android.util.Log
import com.it.fooddonation.data.model.Donation
import com.it.fooddonation.data.model.DonationStatus
import com.it.fooddonation.data.model.FoodItem
import com.it.fooddonation.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * DTO for inserting donations into Supabase
 */
@Serializable
data class DonationDTO(
    val id: String,
    @SerialName("donor_id")
    val donorId: String,
    @SerialName("donor_name")
    val donorName: String? = null,
    val description: String,
    val status: String,
    @SerialName("receiver_id")
    val receiverId: String? = null,
    @SerialName("receiver_name")
    val receiverName: String? = null,
    @SerialName("people_served")
    val peopleServed: Int? = null
)

/**
 * DTO for inserting food items into Supabase
 */
@Serializable
data class FoodItemDTO(
    val id: String,
    @SerialName("donation_id")
    val donationId: String,
    @SerialName("food_name")
    val foodName: String,
    val quantity: String,
    @SerialName("image_url")
    val imageUrl: String? = null
)

/**
 * Repository for managing food donations
 * Connects to Supabase database
 */
class DonationRepository {
    private val supabase = SupabaseClient.client

    companion object {
        private const val TAG = "DonationRepository"

        @Volatile
        private var instance: DonationRepository? = null

        fun getInstance(): DonationRepository {
            return instance ?: synchronized(this) {
                instance ?: DonationRepository().also { instance = it }
            }
        }
    }

    /**
     * Get all donations for a specific donor
     */
    suspend fun getDonationsByDonor(donorId: String): List<Donation> {
        return try {
            Log.d(TAG, "getDonationsByDonor: Fetching donations for donor: $donorId")

            // Fetch donations
            val donationDTOs = supabase.from("donations")
                .select {
                    filter {
                        eq("donor_id", donorId)
                    }
                }
                .decodeList<DonationDTO>()

            Log.d(TAG, "getDonationsByDonor: Found ${donationDTOs.size} donations")

            // For each donation, fetch its food items
            donationDTOs.map { donationDTO ->
                val foodItems = getFoodItemsForDonation(donationDTO.id)
                Donation(
                    id = donationDTO.id,
                    donorId = donationDTO.donorId,
                    donorName = donationDTO.donorName ?: "",
                    foodItems = foodItems,
                    description = donationDTO.description,
                    status = DonationStatus.valueOf(donationDTO.status),
                    receiverId = donationDTO.receiverId,
                    receiverName = donationDTO.receiverName,
                    peopleServed = donationDTO.peopleServed,
                    createdAt = System.currentTimeMillis() // TODO: parse from database
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "getDonationsByDonor: Failed", e)
            emptyList()
        }
    }

    /**
     * Get food items for a donation
     */
    private suspend fun getFoodItemsForDonation(donationId: String): List<FoodItem> {
        return try {
            val foodItemDTOs = supabase.from("food_items")
                .select {
                    filter {
                        eq("donation_id", donationId)
                    }
                }
                .decodeList<FoodItemDTO>()

            foodItemDTOs.map { dto ->
                FoodItem(
                    id = dto.id,
                    foodName = dto.foodName,
                    quantity = dto.quantity,
                    imageUri = dto.imageUrl
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "getFoodItemsForDonation: Failed for donation $donationId", e)
            emptyList()
        }
    }

    /**
     * Create a new donation with food items
     */
    suspend fun createDonation(donation: Donation): Result<Donation> {
        return try {
            Log.d(TAG, "createDonation: Creating donation with ${donation.foodItems.size} food items")

            // Insert donation
            val donationDTO = DonationDTO(
                id = donation.id,
                donorId = donation.donorId,
                donorName = donation.donorName,
                description = donation.description,
                status = donation.status.name,
                receiverId = donation.receiverId,
                receiverName = donation.receiverName,
                peopleServed = donation.peopleServed
            )

            supabase.from("donations").insert(donationDTO)
            Log.d(TAG, "createDonation: Donation inserted successfully")

            // Insert food items
            donation.foodItems.forEach { foodItem ->
                val foodItemDTO = FoodItemDTO(
                    id = foodItem.id,
                    donationId = donation.id,
                    foodName = foodItem.foodName,
                    quantity = foodItem.quantity,
                    imageUrl = foodItem.imageUri
                )
                supabase.from("food_items").insert(foodItemDTO)
            }

            Log.d(TAG, "createDonation: All food items inserted successfully")
            Result.success(donation)
        } catch (e: Exception) {
            Log.e(TAG, "createDonation: Failed", e)
            Result.failure(e)
        }
    }

    /**
     * Update the status of a donation
     */
    suspend fun updateDonationStatus(
        donationId: String,
        newStatus: DonationStatus
    ): Result<Unit> {
        return try {
            Log.d(TAG, "updateDonationStatus: Updating donation $donationId to $newStatus")

            val updateData = buildJsonObject {
                put("status", newStatus.name)
            }

            supabase.from("donations")
                .update(updateData) {
                    filter {
                        eq("id", donationId)
                    }
                }

            Log.d(TAG, "updateDonationStatus: Update successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateDonationStatus: Failed", e)
            Result.failure(e)
        }
    }

    /**
     * Cancel a donation (delete it from database)
     */
    suspend fun cancelDonation(donationId: String): Result<Unit> {
        return try {
            Log.d(TAG, "cancelDonation: Deleting donation $donationId")

            // Delete donation (food_items will be cascade deleted)
            supabase.from("donations")
                .delete {
                    filter {
                        eq("id", donationId)
                    }
                }

            Log.d(TAG, "cancelDonation: Delete successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "cancelDonation: Failed", e)
            Result.failure(e)
        }
    }

    /**
     * Get all available donations (for receivers to browse)
     */
    suspend fun getAvailableDonations(): List<Donation> {
        return try {
            Log.d(TAG, "getAvailableDonations: Fetching all available donations")

            // Fetch donations with AVAILABLE status
            val donationDTOs = supabase.from("donations")
                .select {
                    filter {
                        eq("status", DonationStatus.AVAILABLE.name)
                    }
                }
                .decodeList<DonationDTO>()

            Log.d(TAG, "getAvailableDonations: Found ${donationDTOs.size} available donations")

            // For each donation, fetch its food items
            donationDTOs.map { donationDTO ->
                val foodItems = getFoodItemsForDonation(donationDTO.id)
                Donation(
                    id = donationDTO.id,
                    donorId = donationDTO.donorId,
                    donorName = donationDTO.donorName ?: "",
                    foodItems = foodItems,
                    description = donationDTO.description,
                    status = DonationStatus.valueOf(donationDTO.status),
                    receiverId = donationDTO.receiverId,
                    receiverName = donationDTO.receiverName,
                    peopleServed = donationDTO.peopleServed,
                    createdAt = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAvailableDonations: Failed", e)
            emptyList()
        }
    }

    /**
     * Accept a donation (receiver claims it)
     */
    suspend fun acceptDonation(
        donationId: String,
        receiverId: String,
        receiverName: String
    ): Result<Unit> {
        return try {
            Log.d(TAG, "acceptDonation: Receiver $receiverId accepting donation $donationId")

            val updateData = buildJsonObject {
                put("status", DonationStatus.ACCEPTED.name)
                put("receiver_id", receiverId)
                put("receiver_name", receiverName)
            }

            supabase.from("donations")
                .update(updateData) {
                    filter {
                        eq("id", donationId)
                    }
                }

            Log.d(TAG, "acceptDonation: Successfully accepted")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "acceptDonation: Failed", e)
            Result.failure(e)
        }
    }

    /**
     * Mark donation as picked up and set people served count
     */
    suspend fun markAsPickedUp(
        donationId: String,
        peopleServed: Int
    ): Result<Unit> {
        return try {
            Log.d(TAG, "markAsPickedUp: Updating donation $donationId - $peopleServed people served")

            val updateData = buildJsonObject {
                put("status", DonationStatus.PICKED.name)
                put("people_served", peopleServed)
            }

            supabase.from("donations")
                .update(updateData) {
                    filter {
                        eq("id", donationId)
                    }
                }

            Log.d(TAG, "markAsPickedUp: Successfully updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "markAsPickedUp: Failed", e)
            Result.failure(e)
        }
    }

    /**
     * Get all donations accepted by a receiver
     */
    suspend fun getMyAcceptedDonations(receiverId: String): List<Donation> {
        return try {
            Log.d(TAG, "getMyAcceptedDonations: Fetching donations for receiver: $receiverId")

            // Fetch donations where receiver_id matches
            val donationDTOs = supabase.from("donations")
                .select {
                    filter {
                        eq("receiver_id", receiverId)
                    }
                }
                .decodeList<DonationDTO>()

            Log.d(TAG, "getMyAcceptedDonations: Found ${donationDTOs.size} donations")

            // For each donation, fetch its food items
            donationDTOs.map { donationDTO ->
                val foodItems = getFoodItemsForDonation(donationDTO.id)
                Donation(
                    id = donationDTO.id,
                    donorId = donationDTO.donorId,
                    donorName = donationDTO.donorName ?: "",
                    foodItems = foodItems,
                    description = donationDTO.description,
                    status = DonationStatus.valueOf(donationDTO.status),
                    receiverId = donationDTO.receiverId,
                    receiverName = donationDTO.receiverName,
                    peopleServed = donationDTO.peopleServed,
                    createdAt = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "getMyAcceptedDonations: Failed", e)
            emptyList()
        }
    }
}
