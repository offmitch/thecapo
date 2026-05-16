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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Service
public class MusicService {

    @Autowired
    private SpotifyAuthService authService;

    private Map<String, List<Map<String, String>>> artistCache = new HashMap<>();
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

    public List<Map<String, String>> getSongPoolFromArtist(String artistName) {
        List<Map<String, String>> songPool = new ArrayList<>();
        
        if (artistCache.containsKey(artistName)) {
            System.out.println("Cache hit for artist: " + artistName);
            return artistCache.get(artistName);
        }

        try {
            String token = authService.getAccessToken();

            // Step 1: Search for albums
            String encodedArtist = URLEncoder.encode(artistName, "UTF-8");
            String albumSearchUrl = "https://api.spotify.com/v1/search?q="
                    + encodedArtist
                    + "&type=album&limit=5&market=CA";

            System.out.println("Fetching albums: " + albumSearchUrl);

            HttpURLConnection conn = (HttpURLConnection) new URI(albumSearchUrl).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            String response = readStream(getStream(conn));
            JsonNode json = mapper.readTree(response);
            JsonNode albums = json.path("albums").path("items");

            System.out.println("Albums found: " + albums.size());
            Set<String> seenAlbums = new HashSet<>();
            // Step 2: Loop through albums
            for (JsonNode album : albums) {
                String albumId = album.path("id").asText();
                String albumName = album.path("name").asText();

                if (seenAlbums.contains(albumId)) {
                    continue;
                }
                seenAlbums.add(albumId);

                System.out.println("---- Album: " + albumName + " ----");

                // Step 3: Get tracks for each album
                String tracksUrl = "https://api.spotify.com/v1/albums/"
                        + albumId
                        + "/tracks?limit=50";

                HttpURLConnection trackConn = (HttpURLConnection) new URI(tracksUrl).toURL().openConnection();
                trackConn.setRequestMethod("GET");
                trackConn.setRequestProperty("Authorization", "Bearer " + token);

                String trackResponse = readStream(getStream(trackConn));
                JsonNode trackJson = mapper.readTree(trackResponse);
                JsonNode tracks = trackJson.path("items");

                // Step 4: Add tracks to pool
                for (JsonNode t : tracks) {
                    String title = t.path("name").asText();
                    String artist = t.path("artists").get(0).path("name").asText();

                    Map<String, String> song = Map.of(
                            "title", title,
                            "artist", artist,
                            "album", albumName);

                    songPool.add(song);

                }
                 Thread.sleep(150);
            }

            System.out.println("========== FINAL SONG POOL ==========");
            System.out.println("Total songs collected: " + songPool.size());
            artistCache.put(artistName, songPool);

            return songPool;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private Map<String, String> getHiddenGemFromArtist(
            String artistName,
            int inputYear,
            String originalInput) {

        getSongPoolFromArtist(artistName);

        try {
            String token = authService.getAccessToken();

            String query = artistName;

            // String encodedQuery = URLEncoder.encode(query, "UTF-8")
            // .replace("+", "%20");

            // String urlStr = "https://api.spotify.com/v1/search?q="
            // + encodedQuery
            // + "&type=track"
            // + "&limit=10"
            // + "&market=CA";

            // System.out.println("Query: " + query);
            // System.out.println("URL: " + urlStr);

            // URL url = new URI(urlStr).toURL();
            // HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // conn.setUseCaches(false);
            // conn.setDoInput(true);

            // conn.setRequestMethod("GET");
            // conn.setRequestProperty("Authorization", "Bearer " + token);
            // conn.setRequestProperty("Content-Type", "application/json");

            // InputStream stream = getStream(conn);
            // String response = readStream(stream);
            // stream.close();
            // conn.disconnect();

            // JsonNode json = mapper.readTree(response);
            // JsonNode items = json.path("tracks").path("items");\

            List<Map<String, String>> items = getSongPoolFromArtist(artistName);

            if (items.isEmpty()) {
                System.out.println("No tracks found.");
                return fallback(artistName);
            }

            // System.out.println("Tracks returned:");
            // for (Map<String, String> t : items) {
            //     System.out.println("- " + t.get("title") + " - " + t.get("artist"));
            // }

            String inputTrackName = originalInput.toLowerCase();

            // remove " - artist"
            if (inputTrackName.contains(" - ")) {
                inputTrackName = inputTrackName.split(" - ")[0];
            }

            System.out.println("Cleaned input track name: " + inputTrackName);
            System.out.println("========== FILTER DEBUG ==========");
            System.out.println("Original input: " + originalInput);
            System.out.println("Normalized input: " + inputTrackName);
            System.out.println("Input year: " + inputYear);
            System.out.println("=================================");

            List<Map<String, String>> candidates = new ArrayList<>();

            for (Map<String, String> t : items) {
                String name = t.get("title");
                String artist = t.get("artist");
                String album = t.get("album");

                String normalizedName = normalize(name);
                String normalizedInput = normalize(inputTrackName);

                

                // ❌ exact match
                if (normalizedName.equals(normalizedInput)) {
                    System.out.println("---- Candidate ----");
                System.out.println("Name: " + name);
                System.out.println("Artist: " + artist);
                    System.out.println("❌ Skipped (exact normalized match)");
                    continue;
                }

                // ❌ partial match
                if (normalizedName.contains(normalizedInput) || normalizedInput.contains(normalizedName)) {
                    System.out.println("---- Candidate ----");
                System.out.println("Name: " + name);
                System.out.println("Artist: " + artist);
                    System.out.println("❌ Skipped (partial normalized match)");
                    continue;
                }


                candidates.add(t);
            }

            System.out.println("========== FINAL CANDIDATES SIZE ==========");
            System.out.println("Number of candidates: " + candidates.size());

            System.out.println("======================================");

            // ✅ Fallback if no year matches
            if (candidates.isEmpty()) {
                for (Map<String, String> t : items) {
                    String name = t.get("title").toLowerCase();
                    if (!name.equals(inputTrackName)) {
                        candidates.add(t);
                    }
                }
            }

            // ✅ Final fallback (very rare)
            if (candidates.isEmpty()) {
                candidates.addAll((Collection<? extends Map<String, String>>) items);
            }

            // ✅ Pick random
            Map<String, String> selectedTrack = candidates.get(new Random().nextInt(candidates.size()));

            String title = selectedTrack.get("title");
            String artist = selectedTrack.get("artist");
            

            System.out.println("Selected track:");
            System.out.println("Title: " + title);
            System.out.println("Artist: " + artist);

            return Map.of(
                    "inputArtist", artistName,
                    "title", title,
                    "artist", artist);

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

            System.out.println("Found track: " + track.path("name").asText());
            System.out.println("Artist: " + artistName);
            System.out.println("Release Year: " + year);

            // Step 3: Call your hidden gem function
            Map<String, String> result = getHiddenGemFromArtist(artistName, year, input);
            String title = result.get("title");
            if (title == null) {
                return Map.of("error", "No recommendation found");
            }

            String artist = result.get("artist");
            String recommendation = title + " - " + artist;

            String imageUrl = getAlbumImage(title, artist);

            return Map.of(
                    "recommendation", recommendation,
                    "title", title,
                    "artist", artist,
                    "imageUrl", imageUrl,
                    "originaltrack", track.path("name").asText() + " - " + artistName);
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
                    "artist", track.path("artists").get(0).path("name").asText());
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

    if (status == 429) {
        String retryAfter = conn.getHeaderField("Retry-After");
        int waitTime = retryAfter != null ? Integer.parseInt(retryAfter) : 2;

        System.out.println("⚠️ Rate limited. Waiting " + waitTime + " seconds...");
        // Thread.sleep(waitTime * 1000);

        // return getStream(conn);
    }

    if (status >= 200 && status < 300) {
        return conn.getInputStream();
    } else {
        System.out.println("Error Response: " + conn.getResponseMessage());
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
                "artist", "Tame Impala");
    }

    private String normalize(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z0-9 ]", "") // remove punctuation (apostrophes, etc.)
                .trim();
    }

    private String getAlbumImage(String title, String artist) {
    try {
        String token = authService.getAccessToken();

        String query = URLEncoder.encode(title + " " + artist, "UTF-8");

        String urlStr = "https://api.spotify.com/v1/search?q="
                + query
                + "&type=track&limit=1&market=CA";

        HttpURLConnection conn = (HttpURLConnection) new URI(urlStr).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token);

        String response = readStream(getStream(conn));
        JsonNode json = mapper.readTree(response);

        JsonNode items = json.path("tracks").path("items");

        if (!items.isArray() || items.size() == 0) {
            return "";
        }

        JsonNode track = items.get(0);

        JsonNode images = track.path("album").path("images");

        if (!images.isArray() || images.size() == 0) {
            return "";
        }

        // ✅ Get smallest image (last one is smallest in Spotify response)
        JsonNode smallestImage = images.get(images.size() - 1);

        return smallestImage.path("url").asText();

    } catch (Exception e) {
        e.printStackTrace();
        return "";
    }
}
}
