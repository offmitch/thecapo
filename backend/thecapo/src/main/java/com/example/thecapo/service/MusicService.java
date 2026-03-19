package com.example.thecapo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Random;

@Service
public class MusicService {

    @Autowired
    private SpotifyAuthService authService;

    private final ObjectMapper mapper = new ObjectMapper();

    // -------------------------
    // 🔍 GET TRACK ID
    // -------------------------
    private String getTrackId(String input) {
        try {
            String token = authService.getAccessToken();

            String[] parts = input.split("-");
            String artist = parts.length > 1 ? parts[0].trim() : "";
            String song = parts.length > 1 ? parts[1].trim() : input;

            String query = "track:" + song + (artist.isEmpty() ? "" : " artist:" + artist);
            String encodedQuery = URLEncoder.encode(query, "UTF-8");

            String urlStr = "https://api.spotify.com/v1/search?q=" + encodedQuery + "&type=track&limit=1&market=CA";

            HttpURLConnection conn = (HttpURLConnection) new URI(urlStr).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            InputStream stream = getStream(conn);
            String response = readStream(stream);

            JsonNode json = mapper.readTree(response);
            JsonNode items = json.path("tracks").path("items");

            if (items.isArray() && items.size() > 0) {
                return items.get(0).path("id").asText();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // -------------------------
    // 🎵 SONG RECOMMENDATION
    // -------------------------
    public Map<String, String> getSongRecommendation(String input) {
        try {
            String token = authService.getAccessToken();

            String trackId = "0wwPcA6wtMf6HUMpIRdeP7";
            // getTrackId(input);
            if (trackId == null) return fallback(input);
            
            System.out.println("Track ID for '" + input + "': " + trackId); // 🔥 debug
            
            String urlStr = "https://api.spotify.com/v1/recommendations?seed_tracks=" + trackId + "&limit=10&market=CA";
            
            System.out.println("FINAL URL: " + urlStr);

            URL url = new URI(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            InputStream stream = getStream(conn);
            String response = readStream(stream);

            JsonNode json = mapper.readTree(response);
            JsonNode tracks = json.path("tracks");

            if (!tracks.isArray() || tracks.size() == 0) return fallback(input);

            JsonNode track = tracks.get(new Random().nextInt(tracks.size()));

            return Map.of(
                    "input", input,
                    "title", track.path("name").asText(),
                    "artist", track.path("artists").get(0).path("name").asText(),
                    "link", track.path("external_urls").path("spotify").asText()
            );

        } catch (Exception e) {
            e.printStackTrace();
            return fallback(input);
        }
    }

    // -------------------------
    // 🌌 MOOD RECOMMENDATION
    // -------------------------
    public Map<String, String> getMoodRecommendation(String mood) {
        try {
            String token = authService.getAccessToken();
            String genre = mapMoodToGenre(mood);

            String urlStr = "https://api.spotify.com/v1/recommendations?seed_genres=" + genre + "&limit=10&market=CA";

            URL url = new URI(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            InputStream stream = getStream(conn);
            String response = readStream(stream);

            JsonNode json = mapper.readTree(response);
            JsonNode tracks = json.path("tracks");

            if (!tracks.isArray() || tracks.size() == 0) return fallback(mood);

            JsonNode track = tracks.get(new Random().nextInt(tracks.size()));

            return Map.of(
                    "input", mood,
                    "title", track.path("name").asText(),
                    "artist", track.path("artists").get(0).path("name").asText(),
                    "link", track.path("external_urls").path("spotify").asText()
            );

        } catch (Exception e) {
            e.printStackTrace();
            return fallback(mood);
        }
    }

    // -------------------------
    // 🧠 HELPERS
    // -------------------------
    private InputStream getStream(HttpURLConnection conn) throws Exception {
    int status = conn.getResponseCode();

    System.out.println("HTTP Status: " + status); // 🔥 debug

    if (status >= 200 && status < 300) {
        return conn.getInputStream();
    } else {
        InputStream errorStream = conn.getErrorStream();

        if (errorStream == null) {
            throw new RuntimeException("No response body. Status: " + status);
        }

        return errorStream;
    }
}

    private String readStream(InputStream stream) throws Exception {
    if (stream == null) {
        throw new RuntimeException("Stream is null (no response body)");
    }

    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
    StringBuilder response = new StringBuilder();
    String line;

    while ((line = reader.readLine()) != null) {
        response.append(line);
    }
    
    System.out.println("Response: " + response); // 🔥 debug
    return response.toString();
}

    private String mapMoodToGenre(String mood) {
        mood = mood.toLowerCase();

        if (mood.contains("happy")) return "pop";
        if (mood.contains("sad")) return "acoustic";
        if (mood.contains("energetic")) return "edm";
        if (mood.contains("chill")) return "indie";

        return "pop";
    }

    private Map<String, String> fallback(String input) {
        return Map.of(
                "input", input,
                "title", "Borderline",
                "artist", "Tame Impala",
                "link", "https://open.spotify.com"
        );
    }
}