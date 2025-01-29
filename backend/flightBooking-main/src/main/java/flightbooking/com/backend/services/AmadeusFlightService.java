package flightbooking.com.backend.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Service
public class AmadeusFlightService {

    private static final String FLIGHT_SEARCH_URL = "https://test.api.amadeus.com/v2/shopping/flight-offers";
    private final RestTemplate restTemplate;
    private final AmadeusAuthService authService;

    public AmadeusFlightService(AmadeusAuthService authService) {
        this.restTemplate = new RestTemplate();
        this.authService = authService;
    }

    public List<Map<String, Object>> searchFlights(String origin, String destination, String departureDate, String currency, boolean nonStop) {
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

        // 📌 Aquí simplemente devolvemos la lista y Spring Boot la convierte en JSON
        return transformResponse(response.getBody());
    }

    public List<Map<String, Object>> transformResponse(String responseBody) {
        try {
            // 📌 Convertir la respuesta en un objeto JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(responseBody);
    
            // 📌 Extraer la lista de vuelos (nodo "data")
            JsonNode data = root.path("data");
    
            // 📌 Crear una lista para almacenar vuelos transformados
            List<Map<String, Object>> flights = new ArrayList<>();
    
            for (JsonNode flight : data) {
                Map<String, Object> flightInfo = new HashMap<>();
                flightInfo.put("id", UUID.randomUUID().toString());

    
                // 📌 Extraer cada sección con funciones separadas
                flightInfo.put("generalData", extractGeneralData(flight));
                flightInfo.put("itineraries", extractItineraries(flight));
                flightInfo.put("amenities", extractAmenities(flight));
    
                flights.add(flightInfo);
            }
    
            return flights;
    
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    private Map<String, Object> extractGeneralData(JsonNode flight) {
        Map<String, Object> generalData = new HashMap<>();
        generalData.put("airline", flight.get("validatingAirlineCodes").get(0).asText());
    
        // 📌 Extraer aeropuertos de salida y llegada
        JsonNode itineraries = flight.get("itineraries").get(0);
        JsonNode firstSegment = itineraries.get("segments").get(0);
        JsonNode lastSegment = itineraries.get("segments").get(itineraries.get("segments").size() - 1);
    
        Map<String, String> departureAirport = new HashMap<>();
        departureAirport.put("code", firstSegment.get("departure").get("iataCode").asText());
        departureAirport.put("time", firstSegment.get("departure").get("at").asText());
    
        Map<String, String> arrivalAirport = new HashMap<>();
        arrivalAirport.put("code", lastSegment.get("arrival").get("iataCode").asText());
        arrivalAirport.put("time", lastSegment.get("arrival").get("at").asText());
    
        generalData.put("departureAirport", departureAirport);
        generalData.put("arrivalAirport", arrivalAirport);
    
        // 📌 Agregar duración del vuelo
        generalData.put("flightSchedule", firstSegment.get("departure").get("at").asText() + " → " + lastSegment.get("arrival").get("at").asText());
    
        // 📌 Extraer precios
        JsonNode price = flight.get("price");
        generalData.put("totalCost", price.get("total").asText() + " " + price.get("currency").asText());
    
        JsonNode travelerPricing = flight.get("travelerPricings").get(0);
        generalData.put("costPerTraveler", travelerPricing.get("price").get("total").asText() + " " + price.get("currency").asText());
    
        return generalData;
    }
    private List<Map<String, Object>> extractItineraries(JsonNode flight) {
        List<Map<String, Object>> itineraryDetails = new ArrayList<>();
        JsonNode itineraries = flight.get("itineraries").get(0);
        JsonNode segments = itineraries.get("segments");
    
        for (int i = 0; i < segments.size(); i++) {
            JsonNode segment = segments.get(i);
            Map<String, Object> segmentInfo = new HashMap<>();
    
            // 📌 Aeropuertos y horarios
            segmentInfo.put("departureAirport", segment.get("departure").get("iataCode").asText());
            segmentInfo.put("departureTime", segment.get("departure").get("at").asText());
            segmentInfo.put("arrivalAirport", segment.get("arrival").get("iataCode").asText());
            segmentInfo.put("arrivalTime", segment.get("arrival").get("at").asText());
    
            // 📌 Aerolínea, vuelo y avión
            segmentInfo.put("airline", segment.get("carrierCode").asText());
            segmentInfo.put("flightNumber", segment.get("number").asText());
            segmentInfo.put("aircraft", segment.get("aircraft").get("code").asText());
    
            // 📌 Aerolínea operadora (si es diferente de la principal)
            if (segment.has("operating") && segment.get("operating").has("carrierCode")) {
                segmentInfo.put("operatingAirline", segment.get("operating").get("carrierCode").asText());
            }
    
            // 📌 Duración del segmento
            segmentInfo.put("duration", segment.get("duration").asText());
            // 📌 Detalles de cabina, clase y tarifa
            JsonNode travelerPricing = flight.get("travelerPricings").get(0);
            JsonNode fareDetails = travelerPricing.get("fareDetailsBySegment");

            for (JsonNode fareSegment : fareDetails) {
                if (fareSegment.get("segmentId").asText().equals(segment.get("id").asText())) {
                    segmentInfo.put("cabin", fareSegment.get("cabin").asText());
                    segmentInfo.put("class", fareSegment.get("class").asText());
                    segmentInfo.put("fareBasis", fareSegment.get("fareBasis").asText());
                }
            }

    
            // 📌 Tiempo de layover (escala) si hay otro segmento después
            if (i < segments.size() - 1) {
                JsonNode nextSegment = segments.get(i + 1);
                String arrivalTime = segment.get("arrival").get("at").asText();
                String nextDepartureTime = nextSegment.get("departure").get("at").asText();
                segmentInfo.put("layoverTime", calculateLayoverTime(arrivalTime, nextDepartureTime));
            }
    
            itineraryDetails.add(segmentInfo);
        }
    
        return itineraryDetails;
    }
    private String calculateLayoverTime(String arrivalTime, String nextDepartureTime) {
    try {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime arrival = LocalDateTime.parse(arrivalTime, formatter);
        LocalDateTime nextDeparture = LocalDateTime.parse(nextDepartureTime, formatter);
        Duration layoverDuration = Duration.between(arrival, nextDeparture);

        return String.format("%dH %dM", layoverDuration.toHours(), layoverDuration.toMinutesPart());
    } catch (Exception e) {
        return "N/A";
    }
}

private Map<String, Object> extractAmenities(JsonNode flight) {
    Map<String, Object> amenities = new HashMap<>();
    List<Map<String, Object>> amenityList = new ArrayList<>();

    JsonNode travelerPricing = flight.get("travelerPricings").get(0);
    JsonNode fareDetails = travelerPricing.get("fareDetailsBySegment");

    for (JsonNode fareSegment : fareDetails) {
        if (fareSegment.has("amenities")) {
            for (JsonNode amenity : fareSegment.get("amenities")) {
                Map<String, Object> amenityInfo = new HashMap<>();
                amenityInfo.put("description", amenity.get("description").asText());
                amenityInfo.put("isChargeable", amenity.get("isChargeable").asBoolean());
                amenityInfo.put("type", amenity.get("amenityType").asText());
                amenityList.add(amenityInfo);
            }
        }
    }

    amenities.put("services", amenityList);
    return amenities;
}

    
    
    
    
}

