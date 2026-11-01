package com.seniorinsight.telemetry.access;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class CareServiceAccessClient implements CareAccessClient {
    private final RestTemplate restTemplate;
    private final String careServiceUrl;

    public CareServiceAccessClient(RestTemplate restTemplate,
                                   @Value("${seniorinsight.care-service.base-url:http://localhost:8080}") String careServiceUrl) {
        this.restTemplate = restTemplate;
        this.careServiceUrl = careServiceUrl;
    }

    @Override
    public boolean canReadTelemetry(UUID idosoId, String userId, String role, String bearerToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (bearerToken != null && !bearerToken.isBlank()) headers.setBearerAuth(bearerToken);
            String url = careServiceUrl + "/internal/v1/idosos/" + idosoId + "/access?userId=" + userId + "&role=" + role;
                ResponseEntity<Boolean> response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), Boolean.class);
                return response.getStatusCode().is2xxSuccessful() && Boolean.TRUE.equals(response.getBody());
        } catch (RestClientException | IllegalArgumentException exception) {
            return false;
        }
    }
}
