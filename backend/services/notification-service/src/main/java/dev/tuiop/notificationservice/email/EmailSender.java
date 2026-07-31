package dev.tuiop.notificationservice.email;

public interface EmailSender {

    void sendHtml(String to, String subject, String html);
}
