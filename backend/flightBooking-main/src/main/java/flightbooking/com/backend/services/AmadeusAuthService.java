package flightbooking.com.backend.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import java.util.Map;

@Service
public class AmadeusAuthService {

    private static final String AUTH_URL = "https://test.api.amadeus.com/v1/security/oauth2/token";
    private static final String API_KEY = "wGv54s3s3m1xZTaUjkGBc3rh9MRkhX7K";
    private static final String API_SECRET = "Dc94AbXqEByVUYrK";

    private final RestTemplate restTemplate;

    public AmadeusAuthService() {
        this.restTemplate = new RestTemplate();
    }

    public String getAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        String body = "grant_type=client_credentials&client_id=" + API_KEY + "&client_secret=" + API_SECRET;
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(AUTH_URL, HttpMethod.POST, request, Map.class);
        return response.getBody().get("access_token").toString();
    }
}
