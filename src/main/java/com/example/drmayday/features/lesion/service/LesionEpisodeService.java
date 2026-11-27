package com.example.drmayday.features.lesion.service;

import com.example.drmayday.features.lesion.dto.CreateLesionEpisodeRequest;
import com.example.drmayday.features.lesion.dto.LesionEpisodeResponse;
import com.example.drmayday.features.lesion.dto.UpdateLesionEpisodeRequest;

import java.util.List;

public interface LesionEpisodeService {

    LesionEpisodeResponse createLesionEpisode(CreateLesionEpisodeRequest req);

    List<LesionEpisodeResponse> getMyLesionEpisodes(String hoSoManId);

    LesionEpisodeResponse getLesionEpisode(String id);

    LesionEpisodeResponse editLesionEpisode(String id, UpdateLesionEpisodeRequest req);

    void deleteLesionEpisode(String id);
}
