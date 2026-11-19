import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * RainAlertServer = your RainAlert.java + a small web server.
 *
 *   Browser  --GET /api/rain?lat=..&lon=..&cnt=4-->  THIS JAVA SERVER  --GET-->  OpenWeatherMap
 *   Browser  <------------- JSON (willRain + forecast) ---------------  THIS JAVA SERVER
 *
 * The API key stays here in Java, so it is never visible in the browser.
 */
public class RainAlertServer {

    private static final String OWM_ENDPOINT = "https://api.openweathermap.org/data/2.5/forecast";

    // The API key is read from an environment variable called OWM_API_KEY (never written in the code)
    private static final String API_KEY =
            System.getenv("OWM_API_KEY") != null ? System.getenv("OWM_API_KEY") : "_YOUR_OWM_API_KEY_";

    // Cloud hosts like Render give the port in a PORT variable. On your computer it uses 8080.
    private static final int PORT =
            System.getenv("PORT") != null ? Integer.parseInt(System.getenv("PORT")) : 8080;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Route 1: "/"  → send the web page (RainAlert.html) to the browser
        server.createContext("/", RainAlertServer::handlePage);

        // Route 2: "/api/rain"  → our own API: check rain for the given lat/lon
        server.createContext("/api/rain", RainAlertServer::handleRain);

        server.start();
        System.out.println("Rain Alert server is running.");
        System.out.println("Open this in your browser:  http://localhost:" + PORT);
        System.out.println("Press Ctrl + C to stop.");
    }

    // ------------------------------------------------------------------
    // Route 1: serve the HTML page
    // ------------------------------------------------------------------
    private static void handlePage(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestURI().getPath().equals("/")) {
            send(exchange, 404, "text/plain", "Not found");
            return;
        }
        String html = Files.readString(Path.of("RainAlert.html"), StandardCharsets.UTF_8);
        send(exchange, 200, "text/html; charset=utf-8", html);
    }

    // ------------------------------------------------------------------
    // Route 2: GET /api/rain?lat=12.93&lon=77.58&cnt=4
    // ------------------------------------------------------------------
    private static void handleRain(HttpExchange exchange) throws IOException {
        try {
            // 1. Read lat, lon, cnt sent by the browser
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            String lat = query.get("lat");
            String lon = query.get("lon");
            String cnt = query.getOrDefault("cnt", "4");

            if (lat == null || lon == null) {
                sendError(exchange, 400, "Please send both lat and lon.");
                return;
            }
            double latValue = Double.parseDouble(lat);
            double lonValue = Double.parseDouble(lon);
            if (latValue < -90 || latValue > 90 || lonValue < -180 || lonValue > 180) {
                sendError(exchange, 400, "Latitude must be -90 to 90 and longitude -180 to 180.");
                return;
            }
            if (API_KEY.equals("_YOUR_OWM_API_KEY_")) {
                sendError(exchange, 500, "API key missing. Add it in RainAlertServer.java or set OWM_API_KEY.");
                return;
            }

            // 2. Same steps as RainAlert.java
            JSONObject result = checkRain(lat, lon, cnt);
            send(exchange, 200, "application/json", result.toString());

        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Latitude and longitude must be numbers.");
        } catch (OwmException e) {
            sendError(exchange, e.status, e.getMessage());
        } catch (Exception e) {
            sendError(exchange, 502, "Could not reach OpenWeatherMap: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // YOUR ORIGINAL LOGIC (from RainAlert.java), now inside a method
    // ------------------------------------------------------------------
    private static JSONObject checkRain(String lat, String lon, String cnt)
            throws IOException, InterruptedException, OwmException {

        // Same parameters as your code, but lat/lon/cnt now come from the browser
        Map<String, String> weatherParams = new LinkedHashMap<>();
        weatherParams.put("lat", lat);
        weatherParams.put("lon", lon);
        weatherParams.put("appid", API_KEY);
        weatherParams.put("cnt", cnt);

        String queryString = weatherParams.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OWM_ENDPOINT + "?" + queryString))
                .GET()
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new OwmException(response.statusCode(), friendlyError(response.statusCode()));
        }

        JSONObject weatherData = new JSONObject(response.body());

        boolean willRain = false;
        JSONArray forecastList = weatherData.getJSONArray("list");
        for (int i = 0; i < forecastList.length(); i++) {
            JSONObject hourData = forecastList.getJSONObject(i);
            int conditionCode = hourData.getJSONArray("weather").getJSONObject(0).getInt("id");
            if (conditionCode < 700) {
                willRain = true;
            }
        }

        // Instead of System.out.println, we send the answer back to the browser as JSON
        JSONObject result = new JSONObject();
        result.put("willRain", willRain);
        result.put("message", willRain
                ? "It's going to rain today. Remember to bring an ☔️"
                : "No rain expected today.");
        result.put("lat", lat);
        result.put("lon", lon);
        result.put("city", weatherData.optJSONObject("city"));
        result.put("list", forecastList);          // full forecast, for the timeline cards
        return result;
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------
    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) return map;
        for (String pair : rawQuery.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            map.put(key, value);
        }
        return map;
    }

    private static String friendlyError(int status) {
        if (status == 401) return "Invalid API key. New keys can take up to 2 hours to activate.";
        if (status == 404) return "Location not found.";
        if (status == 429) return "Too many requests. Wait a minute and try again.";
        if (status >= 500) return "OpenWeatherMap server problem. Try again later.";
        return "HTTP error " + status;
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        JSONObject error = new JSONObject();
        error.put("error", message);
        error.put("status", status);
        send(exchange, status, "application/json", error.toString());
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    /** Error coming from OpenWeatherMap, with its HTTP status code. */
    private static class OwmException extends Exception {
        final int status;
        OwmException(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}
