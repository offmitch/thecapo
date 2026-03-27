package com.example.thecapo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Base64;

@Service
public class SpotifyAuthService {

    @Value("${spotify.client.id}")
    private String CLIENT_ID;

    @Value("${spotify.client.secret}")
    private String CLIENT_SECRET;

    
    private String accessToken;
    private long expiryTime;

    public String getAccessToken() {
        if (accessToken == null || System.currentTimeMillis() >= expiryTime) {
            fetchAccessToken();
        }
        // System.out.println("Spotify Access Token: " + accessToken);
        return accessToken;
        
    }

    private void fetchAccessToken() {
        try {
            String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());

            URL url = new URI("https://accounts.spotify.com/api/token").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Basic " + encoded);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String body = "grant_type=client_credentials";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            String response = reader.lines().reduce("", (a, b) -> a + b);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response);

            accessToken = json.get("access_token").asText();
            int expiresIn = json.get("expires_in").asInt();

            expiryTime = System.currentTimeMillis() + (expiresIn - 60) * 1000;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}