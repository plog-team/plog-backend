package com.plog.api.domain.photo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.plog.api.common.UserContext;
import com.plog.api.common.exception.BadRequestException;
import com.plog.api.common.exception.NotFoundException;
import com.plog.api.domain.photo.dto.PhotoUploadBatchResponse;
import com.plog.api.domain.photo.dto.PhotoUploadResponse;

import lombok.RequiredArgsConstructor;
import com.plog.api.domain.photo.dto.PhotoAutoInputContext;
@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private static final int MAX_BATCH = 10;

    private final PhotoService photoService;
    private final PhotoRepository photoRepository;
    private final PhotoLocationRepository photoLocationRepository;

    @PostMapping
    public PhotoUploadBatchResponse upload(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        long userId = UserContext.get();
        List<PhotoUploadResponse> results = new ArrayList<>();

        if (files != null && files.length > 0) {
            if (files.length > MAX_BATCH) {
                throw new BadRequestException("한 번에 최대 " + MAX_BATCH + "장까지 업로드 가능합니다 (요청 " + files.length + "장)");
            }
            for (MultipartFile f : files) {
                results.add(photoService.upload(userId, f));
            }
        } else if (file != null && !file.isEmpty()) {
            results.add(photoService.upload(userId, file));
        } else {
            throw new BadRequestException("files 또는 file 파라미터에 1장 이상 업로드 필요");
        }
        return new PhotoUploadBatchResponse(results);
    }

    @GetMapping("/{photoId}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long photoId) throws IOException {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new NotFoundException("사진을 찾을 수 없습니다: " + photoId));
        Path path = Paths.get(photo.getStoredPath());
        if (!Files.exists(path)) {
            throw new NotFoundException("사진 파일이 없습니다: " + photoId);
        }
        byte[] bytes = Files.readAllBytes(path);
        MediaType mediaType = MediaType.parseMediaType(
                photo.getMimeType() != null ? photo.getMimeType() : "image/jpeg");
        return ResponseEntity.ok().contentType(mediaType).body(bytes);
    }
    @GetMapping("/{photoId}/auto-input")
    public PhotoAutoInputContext getAutoInput(@PathVariable Long photoId) {
        PhotoLocation location = photoLocationRepository.findByPhotoId(photoId)
                .orElseThrow(() -> new NotFoundException("사진 자동입력 정보를 찾을 수 없습니다: " + photoId));

        return PhotoAutoInputContext.builder()
                .photoId(photoId)
                .capturedAt(location.getTakenAt())
                .date(location.getTakenAt() == null ? null : location.getTakenAt().toLocalDate())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .locationHint(location.getLocationName())
                .weather(location.getWeather())
                .temperature(location.getTemperature())
                .build();
    }
    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable Long photoId,
            @RequestHeader("X-User-Id") Long userId){
            photoService.deletePhoto(photoId, userId);
            return ResponseEntity.noContent().build();
    }
}
