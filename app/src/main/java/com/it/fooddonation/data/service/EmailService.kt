package com.it.fooddonation.data.service

import android.util.Log
import com.it.fooddonation.data.model.Donation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailService {

    private const val TAG = "EmailService"

    // TODO: Replace with your Gmail address and App Password
    private const val SENDER_EMAIL = "email"
    private const val APP_PASSWORD = "password"

    suspend fun sendDonationAcceptedEmail(
        recipientEmail: String,
        donorName: String,
        receiverName: String,
        donation: Donation
    ) {
        withContext(Dispatchers.IO) {
            try {
                val properties = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }

                val session = Session.getInstance(properties, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD)
                    }
                })

                val foodItemsRows = donation.foodItems.joinToString("") { item ->
                    """
                    <tr>
                        <td style="padding: 8px; border: 1px solid #ddd;">${item.foodName}</td>
                        <td style="padding: 8px; border: 1px solid #ddd;">${item.quantity}</td>
                    </tr>
                    """.trimIndent()
                }

                val htmlBody = """
                    <html>
                    <body style="font-family: Arial, sans-serif; color: #333;">
                        <h2 style="color: #4CAF50;">Your Donation Has Been Accepted!</h2>
                        <p>Dear <strong>$donorName</strong>,</p>
                        <p><strong>$receiverName</strong> has accepted your donation.</p>
                        <h3>Donated Items</h3>
                        <table style="border-collapse: collapse; width: 100%; max-width: 400px;">
                            <tr style="background-color: #4CAF50; color: white;">
                                <th style="padding: 8px; border: 1px solid #ddd;">Food Item</th>
                                <th style="padding: 8px; border: 1px solid #ddd;">Quantity</th>
                            </tr>
                            $foodItemsRows
                        </table>
                        <p style="margin-top: 16px;">Thank you for your generous contribution!</p>
                        <p style="color: #888; font-size: 12px;">— Food Donation App</p>
                    </body>
                    </html>
                """.trimIndent()

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(SENDER_EMAIL, "Food Donation App"))
                    setRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
                    subject = "Your Donation Has Been Accepted!"
                    setContent(htmlBody, "text/html; charset=utf-8")
                }

                Transport.send(message)
                Log.d(TAG, "Email sent successfully to $recipientEmail")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send email to $recipientEmail: ${e.message}", e)
            }
        }
    }
}
