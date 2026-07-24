package dev.tuiop.notificationservice.email;

import com.sendgrid.SendGrid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SendGridConfig {

    @Bean
    public SendGrid sendGrid(SendGridProperties properties){
        return new SendGrid(properties.apiKey());
    }
}
