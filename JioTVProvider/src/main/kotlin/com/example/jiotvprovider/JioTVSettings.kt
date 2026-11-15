package com.example.jiotvprovider

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Small login dialog opened from CloudStream's plugin settings button. */
class JioTVSettings(private val provider: JioTVProvider) {
    fun show(context: Context) {
        if (provider.isLoggedIn()) {
            AlertDialog.Builder(context)
                .setTitle("JioTV")
                .setMessage("You are signed in to JioTV on this device.")
                .setPositiveButton("Log out") { _, _ ->
                    provider.logout()
                    Toast.makeText(context, "Logged out of JioTV", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Close", null)
                .show()
            return
        }

        val mobileInput = EditText(context).apply {
            hint = "10-digit Jio mobile number"
            inputType = InputType.TYPE_CLASS_PHONE
            maxLines = 1
        }
        val otpInput = EditText(context).apply {
            hint = "6-digit SMS code"
            inputType = InputType.TYPE_CLASS_NUMBER
            maxLines = 1
            visibility = View.GONE
        }
        val padding = (16 * context.resources.displayMetrics.density).toInt()
        val fields = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, 0, padding, 0)
            addView(mobileInput)
            addView(otpInput)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("JioTV sign in")
            .setMessage("Enter your number, request an SMS code, then verify it.")
            .setView(fields)
            .setPositiveButton("Send OTP", null)
            .setNeutralButton("Verify", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val sendButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val verifyButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            verifyButton.isEnabled = false
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

            sendButton.setOnClickListener {
                val mobile = mobileInput.text.toString().filter(Char::isDigit)
                if (mobile.length != 10) {
                    Toast.makeText(context, "Enter a valid 10-digit number", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                sendButton.isEnabled = false
                scope.launch {
                    val result = runCatching { withContext(Dispatchers.IO) { provider.sendOtp(mobile) } }
                    sendButton.isEnabled = true
                    result.onSuccess { sent ->
                        if (sent) {
                            otpInput.visibility = View.VISIBLE
                            verifyButton.isEnabled = true
                            otpInput.requestFocus()
                            Toast.makeText(context, "OTP sent. Enter the SMS code.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "JioTV did not send an OTP. Check the number and try again.", Toast.LENGTH_LONG).show()
                        }
                    }.onFailure {
                        Toast.makeText(context, "Could not request an OTP: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            verifyButton.setOnClickListener {
                val mobile = mobileInput.text.toString().filter(Char::isDigit)
                val otp = otpInput.text.toString().filter(Char::isDigit)
                if (mobile.length != 10 || otp.length != 6) {
                    Toast.makeText(context, "Enter the 10-digit number and 6-digit SMS code", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                verifyButton.isEnabled = false
                scope.launch {
                    val result = runCatching { withContext(Dispatchers.IO) { provider.verifyOtp(mobile, otp) } }
                    verifyButton.isEnabled = true
                    result.onSuccess { message ->
                        if (message == "success") {
                            Toast.makeText(context, "Signed in to JioTV", Toast.LENGTH_LONG).show()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }.onFailure {
                        Toast.makeText(context, "Could not verify the OTP: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }
}
