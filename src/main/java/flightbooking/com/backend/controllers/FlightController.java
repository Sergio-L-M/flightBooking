package flightbooking.com.backend.controllers;
import flightbooking.com.backend.services.AmadeusRawService;
import flightbooking.com.backend.services.AmadeusFlightService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final AmadeusFlightService flightService;
    private final AmadeusRawService rawService;


    public FlightController(AmadeusFlightService flightService, AmadeusRawService rawService) {
        this.flightService = flightService;
        this.rawService = rawService;
    }

    @GetMapping
    public List<Map<String, Object>> getFlights(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam String departureDate,
            @RequestParam(defaultValue = "USD") String currency,
            @RequestParam(defaultValue = "false") boolean nonStop) {

        return flightService.searchFlights(origin, destination, departureDate, currency, nonStop);
    }
    @GetMapping("/raw")
    public String getRawFlights(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam String departureDate,
            @RequestParam(defaultValue = "USD") String currency,
            @RequestParam(defaultValue = "false") boolean nonStop) {

        return rawService.getRawResponse(origin, destination, departureDate, currency, nonStop);
    }
}
