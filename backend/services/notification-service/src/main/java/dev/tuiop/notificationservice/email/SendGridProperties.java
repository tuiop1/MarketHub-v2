package dev.tuiop.notificationservice.email;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "sendgrid")
public record SendGridProperties(
    @NotBlank
    String apiKey,
    @Email @NotBlank String fromEmail,
    @NotBlank String fromName
){
}
