package com.it.fooddonation.data.model

/**
 * Represents the current status of a food donation
 */
enum class DonationStatus {
    /**
     * Donation is available and waiting for a receiver
     */
    AVAILABLE,

    /**
     * Donation has been accepted by a receiver
     */
    ACCEPTED,

    /**
     * Donation has been picked up by the receiver
     */
    PICKED,

    /**
     * Donation process is completed
     */
    COMPLETED
}
