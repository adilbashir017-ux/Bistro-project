package logic;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.util.List;
import java.util.Properties;

/**
 * Service for sending emails.
 * Handles all email messages sent by the system.
 */
public class EmailService {

    /** Sender email address */
    private static final String FROM_EMAIL = System.getenv("BISTRO_EMAIL");

    /** App password for the email account */
    private static final String APP_PASSWORD = System.getenv("BISTRO_EMAIL_APP_PASSWORD");



    /**
     * Creates a mail session using Gmail SMTP.
     *
     * @return mail session
     */
    private static Session createSession() {
        if (FROM_EMAIL == null || FROM_EMAIL.isBlank()
                || APP_PASSWORD == null || APP_PASSWORD.isBlank()) {
            throw new IllegalStateException(
                    "Email credentials are not configured. "
                    + "Set BISTRO_EMAIL and BISTRO_EMAIL_APP_PASSWORD environment variables."
            );
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });
    }



    /**
     * Sends a basic email.
     */
    private static void sendEmail(String toEmail, String subject, String body)
            throws MessagingException {

        Session session = createSession();

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }



    /**
     * Sends reservation confirmation email.
     */
    public static void sendConfirmationEmail(String toEmail, int confirmationCode) {
        try {
            sendEmail(
                toEmail,
                "Reservation Confirmed",
                "Thank you for making a reservation with Bistro!\n\n" +
                "Your confirmation code is:\n" +
                confirmationCode + "\n\n" +
                "Please keep this code for future use."
            );
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }



    /**
     * Sends reservation cancellation email.
     */
    public static void sendCancelEmail(String toEmail, int confirmationCode) {
        try {
            sendEmail(
                toEmail,
                "Reservation Cancelled",
                "Your reservation has been cancelled.\n\n" +
                "Confirmation Code: " + confirmationCode
            );
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }


    /**
     * Sends waiting list cancellation email.
     */
    public static void sendWaitingCancelEmail(String toEmail, int confirmationCode) {
        try {
            sendEmail(
                toEmail,
                "Waiting List Cancelled",
                "You have been removed from the waiting list.\n\n" +
                "Confirmation Code: " + confirmationCode
            );
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    /* ================= LOST CODE / INFO ================= */

    /**
     * Sends reservation info email (lost code, updates, etc.).
     */
    public static void LostCodeEmail(String toEmail, List<String> lines) {
        try {
            StringBuilder body = new StringBuilder();

            for (String line : lines) {
                body.append(line).append("\n");
            }

            sendEmail(
                toEmail,
                "Your Reservation Information",
                body.toString()
            );

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }



    /**
     * Sends cancellation email due to no available tables.
     */
    public static void StaffCancelTableEmail(String toEmail) {
        try {
            sendEmail(
                toEmail,
                "Reservation Cancelled",
                "Your reservation was cancelled due to unavailable places."
            );
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends cancellation email due to special day / hours change.
     */
    public static void StaffCancelSDEmail(String toEmail) {
        try {
            sendEmail(
                toEmail,
                "Reservation Cancelled",
                "Your reservation was cancelled due to changes in working hours."
            );
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }



    /**
     * Sends reservation reminder email.
     */
    public static void sendReminderEmail(String toEmail) {
        try {
            sendEmail(
                toEmail,
                "Reservation Reminder",
                "This is a reminder that you have a reservation in 2 hours."
            );
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }



    /**
     * Sends payment receipt email.
     */
    public static void sendPaymentReceiptEmail(
            String toEmail,
            int confirmationCode,
            double price,
            int isSubscriber
    ) {
        try {

            double finalPrice = price;
            String discountLine = "";

            if (isSubscriber != 0) {
                finalPrice = price * 0.9;
                discountLine = "Subscriber discount applied (10%)\n\n";
            }

            sendEmail(
                toEmail,
                "Payment Receipt - Bistro",
                "Confirmation Code: " + confirmationCode + "\n" +
                "Total Paid: ₪" + String.format("%.2f", finalPrice) + "\n\n" +
                discountLine +
                "Thank you!"
            );

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

}