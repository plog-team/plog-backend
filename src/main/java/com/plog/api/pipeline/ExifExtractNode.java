package com.plog.api.pipeline;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.stereotype.Component;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.plog.api.pipeline.dto.ExifResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExifExtractNode {

    public ExifResult extract(byte[] imageBytes) {
        try {
            Metadata md = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageBytes));

            LocalDateTime capturedAt = null;
            String cameraMake = null;
            String cameraModel = null;
            Integer iso = null;
            String exposure = null;
            Integer width = null;
            Integer height = null;

            ExifSubIFDDirectory sub = md.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (sub != null) {
                Date d = sub.getDateOriginal(java.util.TimeZone.getDefault());
                if (d != null) {
                    capturedAt = LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
                }
                if (sub.containsTag(ExifDirectoryBase.TAG_ISO_EQUIVALENT)) {
                    iso = sub.getInteger(ExifDirectoryBase.TAG_ISO_EQUIVALENT);
                }
                if (sub.containsTag(ExifDirectoryBase.TAG_EXPOSURE_TIME)) {
                    exposure = sub.getString(ExifDirectoryBase.TAG_EXPOSURE_TIME);
                }
            }

            ExifIFD0Directory ifd0 = md.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0 != null) {
                cameraMake = ifd0.getString(ExifIFD0Directory.TAG_MAKE);
                cameraModel = ifd0.getString(ExifIFD0Directory.TAG_MODEL);
            }

            Double lat = null;
            Double lon = null;
            GpsDirectory gps = md.getFirstDirectoryOfType(GpsDirectory.class);
            if (gps != null && gps.getGeoLocation() != null) {
                lat = gps.getGeoLocation().getLatitude();
                lon = gps.getGeoLocation().getLongitude();
            }

            JpegDirectory jpeg = md.getFirstDirectoryOfType(JpegDirectory.class);
            if (jpeg != null) {
                if (jpeg.containsTag(JpegDirectory.TAG_IMAGE_WIDTH)) {
                    width = jpeg.getInteger(JpegDirectory.TAG_IMAGE_WIDTH);
                }
                if (jpeg.containsTag(JpegDirectory.TAG_IMAGE_HEIGHT)) {
                    height = jpeg.getInteger(JpegDirectory.TAG_IMAGE_HEIGHT);
                }
            }

            return ExifResult.builder()
                    .capturedAt(capturedAt)
                    .latitude(lat)
                    .longitude(lon)
                    .cameraMake(cameraMake)
                    .cameraModel(cameraModel)
                    .isoSpeed(iso)
                    .exposureTime(exposure)
                    .pixelWidth(width)
                    .pixelHeight(height)
                    .build();
        } catch (Exception e) {
            log.warn("EXIF 추출 실패 (이미지 손상 또는 메타데이터 없음): {}", e.getMessage());
            return ExifResult.empty();
        }
    }
}
