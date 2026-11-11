package com.festdescans.api.adapter.in.web;

import com.festdescans.api.adapter.in.web.security.AdminTokens;
import com.festdescans.api.application.fakes.Fixtures;
import com.festdescans.api.application.fakes.InMemoryDatabase;
import com.festdescans.api.application.fakes.RecordingCaptchaVerifier;
import com.festdescans.api.application.fakes.SequentialIdGenerator;
import com.festdescans.api.domain.admin.AdminUser;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest(classes = WebTestApplication.class, properties = {
        // Same secret the Node-signed fixture tokens in SecurityTest were created with.
        "festpage.web.jwt-secret=" + WebTestBase.JWT_SECRET,
        "festpage.web.cors-origins=https://festdescans.com,https://admin.festdescans.com",
})
@AutoConfigureMockMvc
abstract class WebTestBase {

    static final String JWT_SECRET = "compat-test-secret-0123456789abcdef";

    @Autowired
    protected MockMvcTester mvc;
    @Autowired
    protected InMemoryDatabase db;
    @Autowired
    protected RecordingCaptchaVerifier captcha;
    @Autowired
    protected SequentialIdGenerator ids;
    @Autowired
    protected AdminTokens tokens;

    @BeforeEach
    void resetState() {
        db.clear();
        captcha.reset();
        ids.reset();
    }

    protected String adminToken() {
        return tokens.issue(new AdminUser("admin-1", "admin@festdescans.com", "", "Admin", Fixtures.T0, Fixtures.T0));
    }

    protected String bearer() {
        return "Bearer " + adminToken();
    }
}
