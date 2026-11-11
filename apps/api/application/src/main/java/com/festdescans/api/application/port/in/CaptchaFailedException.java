package com.festdescans.api.application.port.in;

public final class CaptchaFailedException extends RuntimeException {

    public CaptchaFailedException() {
        super("Captcha verification failed");
    }
}
