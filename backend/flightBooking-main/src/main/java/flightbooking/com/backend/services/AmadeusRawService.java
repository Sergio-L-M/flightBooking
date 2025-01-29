package flightbooking.com.backend.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

@Service
public class AmadeusRawService {

    private static final String FLIGHT_SEARCH_URL = "https://test.api.amadeus.com/v2/shopping/flight-offers";
    private final RestTemplate restTemplate;
    private final AmadeusAuthService authService;

    public AmadeusRawService(AmadeusAuthService authService) {
        this.restTemplate = new RestTemplate();
        this.authService = authService;
    }

    public String getRawResponse(String origin, String destination, String departureDate, String currency, boolean nonStop) {
        String token = authService.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        String url = FLIGHT_SEARCH_URL +
                "?originLocationCode=" + origin +
                "&destinationLocationCode=" + destination +
                "&departureDate=" + departureDate +
                "&adults=1" +
                "&currencyCode=" + currency +
                "&nonStop=" + nonStop;

        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

        return response.getBody(); // 📌 Devuelve la respuesta original de Amadeus
    }
}
