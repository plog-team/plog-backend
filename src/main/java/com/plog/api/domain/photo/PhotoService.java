package com.plog.api.domain.photo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.plog.api.common.exception.BadRequestException;
import com.plog.api.domain.cache.ImageAnalysisCache;
import com.plog.api.domain.cache.ImageAnalysisCacheRepository;
import com.plog.api.domain.photo.dto.PhotoUploadResponse;
import com.plog.api.util.ImageResizer;
import com.plog.api.util.Sha256Hasher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final ImageAnalysisCacheRepository cacheRepository;

    @Value("${plog.upload.base-dir:./uploads}")
    private String baseDir;

    @Transactional
    public PhotoUploadResponse upload(long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("파일이 비어있습니다");
        }
        String mime = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");
        if (!mime.startsWith("image/")) {
            throw new BadRequestException("이미지 파일만 업로드 가능합니다 (got: " + mime + ")");
        }

        byte[] rawBytes;
        try {
            rawBytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("파일 읽기 실패: " + e.getMessage());
        }

        String sha = Sha256Hasher.hex(rawBytes);
        boolean cacheHit = cacheRepository.findBySha256(sha).isPresent();

        String format = extractFormat(mime, file.getOriginalFilename());
        ImageResizer.Result resized;
        try {
            resized = ImageResizer.resizeIfTooLarge(rawBytes, format);
        } catch (IOException e) {
            throw new BadRequestException("이미지 디코딩/리사이즈 실패: " + e.getMessage());
        }

        String storedFilename = sha + "." + resized.format();
        Path userDir = Paths.get(baseDir, String.valueOf(userId));
        Path target = userDir.resolve(storedFilename);
        Path originalTarget = userDir.resolve(sha + ".original");
        try {
            Files.createDirectories(userDir);
            if (!Files.exists(target)) {
                Files.write(target, resized.bytes());
            }
            if (!Files.exists(originalTarget)) {
                Files.write(originalTarget, rawBytes);
            }
        } catch (IOException e) {
            throw new BadRequestException("파일 저장 실패: " + e.getMessage());
        }

        Photo photo = photoRepository.save(Photo.builder()
                .userId(userId)
                .sha256(sha)
                .originalFilename(Optional.ofNullable(file.getOriginalFilename()).orElse(storedFilename))
                .mimeType(mime)
                .width(resized.width())
                .height(resized.height())
                .sizeBytes((long) resized.bytes().length)
                .storedPath(target.toString().replace('\\', '/'))
                .build());

        log.info("Uploaded photo id={} userId={} sha={} {}x{} cacheHit={}",
                photo.getId(), userId, sha, resized.width(), resized.height(), cacheHit);

        return PhotoUploadResponse.builder()
                .photoId(photo.getId())
                .sha256(sha)
                .originalFilename(photo.getOriginalFilename())
                .mimeType(mime)
                .width(photo.getWidth())
                .height(photo.getHeight())
                .sizeBytes(photo.getSizeBytes())
                .storedPath(photo.getStoredPath())
                .cacheHit(cacheHit)
                .build();
    }

    @Transactional
    public void deletePhoto(Long photoId, Long userId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사진을 찾을 수 없습니다."));

        if (!photo.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }

        photo.setDeleted(true);
        photoRepository.save(photo);
    }

    private String extractFormat(String mime, String filename) {
        if (mime != null) {
            if (mime.contains("png")) return "png";
            if (mime.contains("webp")) return "webp";
            if (mime.contains("gif")) return "gif";
        }
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot >= 0 && dot < filename.length() - 1) {
                return filename.substring(dot + 1).toLowerCase();
            }
        }
        return "jpg";
    }
}
