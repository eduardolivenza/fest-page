package com.festdescans.api.application.fakes;

import com.festdescans.api.application.port.out.CaptchaVerifier;
import java.util.ArrayList;
import java.util.List;

/** Answers with a configurable result and records every check. */
public final class RecordingCaptchaVerifier implements CaptchaVerifier {

    public record Check(String token, String remoteIp) {
    }

    private final List<Check> checks = new ArrayList<>();
    private boolean result = true;

    @Override
    public boolean verify(String token, String remoteIp) {
        checks.add(new Check(token, remoteIp));
        return result;
    }

    public void answer(boolean passes) {
        this.result = passes;
    }

    public List<Check> checks() {
        return List.copyOf(checks);
    }

    public void reset() {
        checks.clear();
        result = true;
    }
}
