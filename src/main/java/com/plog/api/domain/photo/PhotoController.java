package com.plog.api.domain.photo;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.plog.api.common.UserContext;
import com.plog.api.common.exception.BadRequestException;
import com.plog.api.domain.photo.dto.PhotoUploadBatchResponse;
import com.plog.api.domain.photo.dto.PhotoUploadResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private static final int MAX_BATCH = 10;

    private final PhotoService photoService;

    /**
     * multipart files[] 1~10장 배치 업로드.
     * 단일 file 파라미터도 허용 (하위 호환).
     */
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
}
