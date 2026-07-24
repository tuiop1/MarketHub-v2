package dev.tuiop.notificationservice.email;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SendGridEmailSender implements EmailSender{
    private final SendGrid sendGrid;
    private final SendGridProperties properties;

    @Override
    public void sendHtml(String to, String subject, String html) {

        Email from = new Email(
                properties.fromEmail(),
                properties.fromName()

        );

        Email recipient = new Email(to);
        Content content = new Content("text/html", html);

        Mail mail = new Mail(from, subject, recipient, content);

        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);

            if (response.getStatusCode() < 200
                    || response.getStatusCode() >= 300) {
                throw new EmailDeliveryException(
                        "SendGrid rejected email. status=%s body=%s"
                                .formatted(
                                        response.getStatusCode(),
                                        response.getBody()
                                )
                );
            }
        } catch (IOException exception) {
            throw new EmailDeliveryException(
                    "Could not send email through SendGrid",
                    exception
            );
        }
    }
    }
