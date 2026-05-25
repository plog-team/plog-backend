package com.plog.api.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.coobird.thumbnailator.Thumbnails;

public final class ImageResizer {

    public static final int MAX_DIMENSION = 2048;

    private ImageResizer() {}

    public record Result(byte[] bytes, int width, int height, String format) {}

    public static Result resizeIfTooLarge(byte[] bytes, String fallbackFormat) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
        if (img == null) {
            throw new IOException("이미지를 읽을 수 없습니다 (지원되지 않는 포맷)");
        }
        String format = fallbackFormat == null || fallbackFormat.isBlank() ? "jpg" : fallbackFormat.toLowerCase();
        int origW = img.getWidth();
        int origH = img.getHeight();
        if (origW <= MAX_DIMENSION && origH <= MAX_DIMENSION) {
            return new Result(bytes, origW, origH, format);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(img)
                .size(MAX_DIMENSION, MAX_DIMENSION)
                .keepAspectRatio(true)
                .useExifOrientation(true)  // 모바일 카메라 회전 메타데이터 자동 보정
                .outputFormat(format)
                .toOutputStream(out);
        byte[] resized = out.toByteArray();
        BufferedImage check = ImageIO.read(new ByteArrayInputStream(resized));
        return new Result(resized, check.getWidth(), check.getHeight(), format);
    }
}
