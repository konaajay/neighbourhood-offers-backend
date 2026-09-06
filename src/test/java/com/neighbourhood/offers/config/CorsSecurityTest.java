package com.neighbourhood.offers.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CorsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String NETLIFY_ORIGIN = "https://neighbourhood-offers-frontend.netlify.app";
    private static final String LOCALHOST_ORIGIN = "http://localhost:5173";

    @Test
    @DisplayName("CORS Preflight: Netlify origin is allowed with credentials, methods, and headers")
    void testPreflightNetlifyOrigin() throws Exception {
        mockMvc.perform(options("/api/counter/lookup")
                        .header(HttpHeaders.ORIGIN, NETLIFY_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type, Accept"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, NETLIFY_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
    }

    @Test
    @DisplayName("CORS Preflight: Localhost origin is preserved for local development")
    void testPreflightLocalhostOrigin() throws Exception {
        mockMvc.perform(options("/api/shopper/offers")
                        .header(HttpHeaders.ORIGIN, LOCALHOST_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCALHOST_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("CORS Actual Request: Netlify origin receives Access-Control-Allow-Origin header")
    void testActualRequestWithCors() throws Exception {
        mockMvc.perform(get("/api/shopper/offers")
                        .header(HttpHeaders.ORIGIN, NETLIFY_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, NETLIFY_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("CORS: Unauthorized origin is not granted Access-Control-Allow-Origin")
    void testUnauthorizedOriginRejected() throws Exception {
        mockMvc.perform(options("/api/counter/lookup")
                        .header(HttpHeaders.ORIGIN, "https://malicious-site.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
