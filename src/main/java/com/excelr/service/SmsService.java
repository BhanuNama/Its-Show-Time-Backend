package com.excelr.service;

import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

@Service
public class SmsService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.verify.service.sid}")
    private String verifyServiceSid;

    // Fallback storage for unverified numbers (Twilio Trial limitation)
    private final Map<String, String> localOtpStorage = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (accountSid != null && !accountSid.isEmpty() && authToken != null && !authToken.isEmpty()) {
            Twilio.init(accountSid, authToken);
        }
    }

    /**
     * Sends OTP via Twilio Verify Service.
     * Fallback: If Twilio fails (e.g. unverified number on Trial), generates local
     * OTP and prints to console.
     */
    public String sendOtp(String phoneNumber) {
        try {
            Verification verification = Verification.creator(
                    verifyServiceSid,
                    phoneNumber,
                    "sms")
                    .create();
            return verification.getSid();
        } catch (Exception e) {
            System.err.println("Twilio Verify Failed (Likely Trial Restriction): " + e.getMessage());

            // --- FALLBACK FOR DEV/TRIAL ACCOUNT ---
            String mockOtp = String.format("%06d", new Random().nextInt(999999));
            localOtpStorage.put(phoneNumber, mockOtp);

            System.out.println("\n==================================================");
            System.out.println(" [DEV MODE] TWILIO FALLBACK OTP");
            System.out.println(" Phone: " + phoneNumber);
            System.out.println(" CODE:  " + mockOtp);
            System.out.println("==================================================\n");

            return "mock-sid-" + System.currentTimeMillis();
        }
    }

    /**
     * Verifies the OTP using Twilio Verify Service OR Local Storage.
     */
    public boolean verifyOtp(String phoneNumber, String code) {
        // 1. Check Local Mock Storage first
        if (localOtpStorage.containsKey(phoneNumber)) {
            String storedOtp = localOtpStorage.get(phoneNumber);
            if (storedOtp.equals(code)) {
                localOtpStorage.remove(phoneNumber); // One-time use
                return true;
            }
            // If in local storage but code wrong, don't fallback to Twilio (it won't have
            // it)
            return false;
        }

        // 2. Try Twilio Verify
        try {
            VerificationCheck verificationCheck = VerificationCheck.creator(
                    verifyServiceSid)
                    .setTo(phoneNumber)
                    .setCode(code)
                    .create();

            return "approved".equals(verificationCheck.getStatus());
        } catch (Exception e) {
            System.err.println("Twilio Verification Check Failed: " + e.getMessage());
            return false;
        }
    }
}
