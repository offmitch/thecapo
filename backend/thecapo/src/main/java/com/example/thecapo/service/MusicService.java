package com.example.thecapo.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MusicService {

    public Map<String, String> getSongRecommendation(String song) {
        return Map.of(
                "input", song,
                "recommendation", "Try 'Blinding Lights - The Weeknd'"
        );
    }

    public Map<String, String> getMoodRecommendation(String mood) {

        String recommendation;

        switch (mood.toLowerCase()) {
            case "happy":
                recommendation = "Can't Stop the Feeling - Justin Timberlake";
                break;
            case "sad":
                recommendation = "Someone Like You - Adele";
                break;
            case "energetic":
                recommendation = "Titanium - David Guetta";
                break;
            case "chill":
                recommendation = "Sunflower - Post Malone";
                break;
            default:
                recommendation = "Shape of You - Ed Sheeran";
        }

        return Map.of(
                "input", mood,
                "recommendation", recommendation
        );
    }
}