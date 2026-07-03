package dev.tuiop.accountservice.common.exceptions;

public class EmailAlreadyTakenException extends BusinessException {
    public EmailAlreadyTakenException(String email) {
        super("EMAIL_ALREADY_TAKEN", "Email " + email + " is already taken", 409);
    }
}
