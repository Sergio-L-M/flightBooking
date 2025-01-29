package flightbooking.com.backend;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AmadeusAuthTest {
    
    private static final String API_KEY = "wGv54s3s3m1xZTaUjkGBc3rh9MRkhX7K";
    private static final String API_SECRET = "Dc94AbXqEByVUYrK";
    private static final String AUTH_URL = "https://test.api.amadeus.com/v1/security/oauth2/token";

    public static String getAccessToken() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // Cuerpo de la solicitud de autenticación
        String body = "grant_type=client_credentials&client_id=" + API_KEY + "&client_secret=" + API_SECRET;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String responseBody = response.body();
            System.out.println("🔍 Respuesta completa de Amadeus: " + responseBody); // Agregado para depuración
            String access_token = parseToken(responseBody);
            System.out.println("✅ Token de acceso: " + access_token);
            return access_token;
        }
        
        else {
            System.err.println("❌ Error al obtener el token: " + response.body());
            throw new RuntimeException("No se pudo obtener el token");
        }
    }

private static String parseToken(String responseBody) {
    try {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("access_token").asText(); // Extrae el token correctamente
    } catch (Exception e) {
        System.err.println("❌ Error extrayendo el token: " + e.getMessage());
        return "ERROR";
    }
}
    public static void main(String[] args) {
        try {
            getAccessToken();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
