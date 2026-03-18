package com.example.thecapo.controller;

import com.example.thecapo.service.MusicService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MusicController {

    private final MusicService musicService;

    public MusicController(MusicService musicService) {
        this.musicService = musicService;
    }

    @GetMapping("/recommend")
    public Map<String, String> recommend(
            @RequestParam(required = false) String song,
            @RequestParam(required = false) String mood) {

        if (song != null && !song.isEmpty()) {
            return musicService.getSongRecommendation(song);
        }

        if (mood != null && !mood.isEmpty()) {
            return musicService.getMoodRecommendation(mood);
        }

        return Map.of("error", "Please provide a song or mood");
    }
}