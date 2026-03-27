package com.example.thecapo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    private String getAlbumId(String input) {
        try {
            String token = authService.getAccessToken();

            // Split "Artist - Album" format (optional)
            String[] parts = input.split("-");
            String artist = parts.length > 1 ? parts[0].trim() : "";
            String album = parts.length > 1 ? parts[1].trim() : input;

            // Build album query (IMPORTANT: use album:, not track:)
            String query = "album:" + album + (artist.isEmpty() ? "" : " artist:" + artist);
            String encodedQuery = URLEncoder.encode(query, "UTF-8");

            String urlStr = "https://api.spotify.com/v1/search?q="
                    + encodedQuery
                    + "&type=album"
                    + "&limit=1"
                    + "&market=CA";

            System.out.println("Album search query: " + query);
            System.out.println("Encoded query: " + encodedQuery);
            System.out.println("Request URL: " + urlStr);

            URL url = new URI(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            InputStream stream = getStream(conn);
            String response = readStream(stream);

            JsonNode json = mapper.readTree(response);
            JsonNode items = json.path("albums").path("items");

            if (!items.isArray() || items.size() == 0) {
                System.out.println("No albums found.");
                return null;
            }

            JsonNode albumNode = items.get(0);

            String albumId = albumNode.path("id").asText();
            String albumName = albumNode.path("name").asText();
            String artistName = albumNode.path("artists").get(0).path("name").asText();

            System.out.println("Found album:");
            System.out.println("Name: " + albumName);
            System.out.println("Artist: " + artistName);
            System.out.println("ID: " + albumId);

            return albumId;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getArtistId(String input) {
        try {
            String token = authService.getAccessToken();

            // In case user types "Artist - Something", just take the artist part
            String artist = input.contains("-")
                    ? input.split("-")[0].trim()
                    : input.trim();

            // Build artist query
            String query = "artist:" + artist;
            String encodedQuery = URLEncoder.encode(query, "UTF-8");

            String urlStr = "https://api.spotify.com/v1/search?q="
                    + encodedQuery
                    + "&type=artist"
                    + "&limit=1";

            System.out.println("Artist search query: " + query);
            System.out.println("Encoded query: " + encodedQuery);
            System.out.println("Request URL: " + urlStr);

            URL url = new URI(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            InputStream stream = getStream(conn);
            String response = readStream(stream);

            JsonNode json = mapper.readTree(response);
            JsonNode items = json.path("artists").path("items");

            if (!items.isArray() || items.size() == 0) {
                System.out.println("No artists found.");
                return null;
            }

            JsonNode artistNode = items.get(0);

            String artistId = artistNode.path("id").asText();
            String artistName = artistNode.path("name").asText();

            System.out.println("Found artist:");
            System.out.println("Name: " + artistName);
            System.out.println("ID: " + artistId);

            return artistId;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, String> getHiddenGemFromArtist(
            String artistName,
            int inputYear,
            String originalInput) {

        try {
            String token = authService.getAccessToken();

            String query = artistName;

            String encodedQuery = URLEncoder.encode(query, "UTF-8")
                    .replace("+", "%20");

            String urlStr = "https://api.spotify.com/v1/search?q="
                    + encodedQuery
                    + "&type=track"
                    + "&limit=10"
                    + "&market=CA";

            System.out.println("Query: " + query);
            System.out.println("URL: " + urlStr);

            URL url = new URI(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setUseCaches(false);
            conn.setDoInput(true);

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");

            InputStream stream = getStream(conn);
            String response = readStream(stream);
            stream.close();
            conn.disconnect();

            JsonNode json = mapper.readTree(response);
            JsonNode items = json.path("tracks").path("items");

            if (!items.isArray() || items.size() == 0) {
                System.out.println("No tracks found.");
                return fallback(artistName);
            }

            // ✅ Print all tracks
            System.out.println("Tracks returned:");
            for (JsonNode t : items) {
                System.out.println("- " + t.path("name").asText());
            }

            String inputTrackName = originalInput.toLowerCase();

            List<JsonNode> candidates = new ArrayList<>();

            // ✅ Filter: avoid original + keep similar year range
            for (JsonNode t : items) {
                String name = t.path("name").asText().toLowerCase();

                if (name.equals(inputTrackName))
                    continue;

                String releaseDate = t.path("album").path("release_date").asText();
                if (releaseDate.length() < 4)
                    continue;

                int year = Integer.parseInt(releaseDate.substring(0, 4));

                if (year >= (inputYear - 2) && year <= (inputYear + 2)) {
                    candidates.add(t);
                }
            }

            // ✅ Fallback if no year matches
            if (candidates.isEmpty()) {
                for (JsonNode t : items) {
                    String name = t.path("name").asText().toLowerCase();
                    if (!name.equals(inputTrackName)) {
                        candidates.add(t);
                    }
                }
            }

            // ✅ Final fallback (very rare)
            if (candidates.isEmpty()) {
                candidates.addAll((Collection<? extends JsonNode>) items);
            }

            // ✅ Pick random
            JsonNode selectedTrack = candidates.get(new Random().nextInt(candidates.size()));

            String title = selectedTrack.path("name").asText();
            String artist = selectedTrack.path("artists").get(0).path("name").asText();
            String link = selectedTrack.path("external_urls").path("spotify").asText();

            System.out.println("Selected track:");
            System.out.println("Title: " + title);
            System.out.println("Artist: " + artist);

            return Map.of(
                    "inputArtist", artistName,
                    "title", title,
                    "artist", artist,
                    "link", link);

        } catch (Exception e) {
            e.printStackTrace();
            return fallback(artistName);
        }
    }

    public Map<String, String> getSongRecommendation(String input) {
        try {
            System.out.println("========== SONG RECOMMENDER ==========");
            System.out.println("Input: " + input);

            String token = authService.getAccessToken();

            // Step 1: Search for the track
            String encodedQuery = URLEncoder.encode(input, "UTF-8");

            String urlStr = "https://api.spotify.com/v1/search?q="
                    + encodedQuery
                    + "&type=track"
                    + "&limit=1"
                    + "&market=CA";

            System.out.println("Search URL: " + urlStr);

            URL url = new URI(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");

            InputStream stream = getStream(conn);
            String response = readStream(stream);
            stream.close();

            JsonNode json = mapper.readTree(response);
            JsonNode items = json.path("tracks").path("items");

            if (!items.isArray() || items.size() == 0) {
                System.out.println("No track found.");
                return fallback(input);
            }

            JsonNode track = items.get(0);

            // Step 2: Extract needed data
            String artistName = track.path("artists").get(0).path("name").asText();
            String releaseDate = track.path("album").path("release_date").asText();

            // release_date can be "YYYY-MM-DD" or "YYYY"
            int year = Integer.parseInt(releaseDate.substring(0, 4));

            System.out.println("Found track:");
            System.out.println("Artist: " + artistName);
            System.out.println("Release Year: " + year);

            // Step 3: Call your hidden gem function
            Map<String, String> result = getHiddenGemFromArtist(artistName, year, input);
            String title = result.get("title"); // use the correct key

            if (title == null) {
                return Map.of("error", "No recommendation found");
            }

            return Map.of("recommendation", title, "artist", result.get("artist"), "link", result.get("link"));

        } catch (Exception e) {
            System.out.println("ERROR:");
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

            if (!tracks.isArray() || tracks.size() == 0)
                return fallback(mood);

            JsonNode track = tracks.get(new Random().nextInt(tracks.size()));

            return Map.of(
                    "input", mood,
                    "title", track.path("name").asText(),
                    "artist", track.path("artists").get(0).path("name").asText(),
                    "link", track.path("external_urls").path("spotify").asText());

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

        // System.out.println("HTTP Status: " + status);

        if (status >= 200 && status < 300) {
            return conn.getInputStream();
        } else {
            InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                String error = readStream(errorStream); // now allowed
                System.out.println("Error Response: " + error);
            }
            throw new RuntimeException("HTTP Error: " + status);
        }
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            throw new RuntimeException("Stream is null (no response body)");
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        // System.out.println("Response: " + response);
        return response.toString();
    }

    private String mapMoodToGenre(String mood) {
        mood = mood.toLowerCase();

        if (mood.contains("happy"))
            return "pop";
        if (mood.contains("sad"))
            return "acoustic";
        if (mood.contains("energetic"))
            return "edm";
        if (mood.contains("chill"))
            return "indie";

        return "pop";
    }

    private Map<String, String> fallback(String input) {
        return Map.of(
                "input", input,
                "title", "Borderline",
                "artist", "Tame Impala",
                "link", "https://open.spotify.com");
    }
}