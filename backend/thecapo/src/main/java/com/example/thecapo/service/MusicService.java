package com.example.thecapo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Random;

@Service
public class MusicService {

    @Autowired
    private SpotifyAuthService authService;

    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, String> getSongRecommendation(String song) {
        try {
            String encodedSong = URLEncoder.encode(song, "UTF-8");

            // 1. Search for the song
            String searchUrl = "https://api.spotify.com/v1/search?q=" + encodedSong + "&type=track&limit=1";

            HttpURLConnection searchConn = (HttpURLConnection) new URI(searchUrl).toURL().openConnection();
            searchConn.setRequestMethod("GET");
            searchConn.setRequestProperty("Authorization", "Bearer " + authService.getAccessToken());

            BufferedReader searchReader = new BufferedReader(
                    new InputStreamReader(searchConn.getInputStream())
            );

            String searchResponse = searchReader.lines().reduce("", (a, b) -> a + b);

            JsonNode searchJson = mapper.readTree(searchResponse);
            JsonNode items = searchJson.get("tracks").get("items");

            if (items.isEmpty()) return fallback(song);

            String trackId = items.get(0).get("id").asText();

            // 2. Get recommendations
            String recUrl = "https://api.spotify.com/v1/recommendations?seed_tracks=" + trackId + "&limit=10";

            HttpURLConnection recConn = (HttpURLConnection) new URI(recUrl).toURL().openConnection();
            recConn.setRequestMethod("GET");
            recConn.setRequestProperty("Authorization", "Bearer " + authService.getAccessToken());

            BufferedReader recReader = new BufferedReader(
                    new InputStreamReader(recConn.getInputStream())
            );

            String recResponse = recReader.lines().reduce("", (a, b) -> a + b);

            JsonNode recJson = mapper.readTree(recResponse);
            JsonNode tracks = recJson.get("tracks");

            int randomIndex = new Random().nextInt(tracks.size());
            JsonNode track = tracks.get(randomIndex);

            return Map.of(
                    "input", song,
                    "title", track.get("name").asText(),
                    "artist", track.get("artists").get(0).get("name").asText(),
                    "link", track.get("external_urls").get("spotify").asText()
            );

        } catch (Exception e) {
            e.printStackTrace();
            return fallback(song);
        }
    }

    public Map<String, String> getMoodRecommendation(String mood) {

        String genre = mapMoodToGenre(mood);

        try {
            String urlStr = "https://api.spotify.com/v1/recommendations?seed_genres=" + genre + "&limit=10";

            HttpURLConnection conn = (HttpURLConnection) new URI(urlStr).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + authService.getAccessToken());

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            String response = reader.lines().reduce("", (a, b) -> a + b);

            JsonNode json = mapper.readTree(response);
            JsonNode tracks = json.get("tracks");

            if (tracks.isEmpty()) return fallback(mood);

            int randomIndex = new Random().nextInt(tracks.size());
            JsonNode track = tracks.get(randomIndex);

            return Map.of(
                    "input", mood,
                    "title", track.get("name").asText(),
                    "artist", track.get("artists").get(0).get("name").asText(),
                    "link", track.get("external_urls").get("spotify").asText()
            );

        } catch (Exception e) {
            e.printStackTrace();
            return fallback(mood);
        }
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