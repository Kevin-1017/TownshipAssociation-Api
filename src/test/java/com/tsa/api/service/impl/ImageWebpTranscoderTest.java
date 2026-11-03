package com.tsa.api.service.impl;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 纯逻辑单测（不拉 Spring 上下文）：转码成功路径的产物形状 + 坏字节回退。
 */
class ImageWebpTranscoderTest {

    private static byte[] fakeScreenshot(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(30, 60, 120));
        g.fillRect(0, 0, w, h);
        g.setColor(Color.WHITE);
        g.fillRect(10, 10, w / 2, h / 3);
        g.dispose();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }

    @Test
    void 超大PNG转WebP并钳到长边1080以内() throws Exception {
        byte[] raw = fakeScreenshot(2000, 1500);
        ImageWebpTranscoder.Webp out =
                ImageWebpTranscoder.transcode("0b1e5c2f-9a3d-4e7c-8f21-6d0a4b3c5e77.png", raw);

        assertThat(out).as("编码器缺席会让本用例红——CI/服务器均为 x64，webp-imageio 带 native 库").isNotNull();
        assertThat(out.objectKey()).isEqualTo("0b1e5c2f-9a3d-4e7c-8f21-6d0a4b3c5e77.webp");
        assertThat(out.data()).hasSizeLessThan(raw.length / 4);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(out.data()));
        assertThat(decoded).isNotNull();
        assertThat(Math.max(decoded.getWidth(), decoded.getHeight())).isLessThanOrEqualTo(1080);
        // 等比：2000x1500 → 1080x810
        assertThat(decoded.getWidth()).isEqualTo(1080);
        assertThat(decoded.getHeight()).isEqualTo(810);
    }

    @Test
    void 已达标小图不放大只压缩() throws Exception {
        byte[] raw = fakeScreenshot(640, 360);
        ImageWebpTranscoder.Webp out = ImageWebpTranscoder.transcode("aaaa.png", raw);

        assertThat(out).isNotNull();
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(out.data()));
        assertThat(decoded.getWidth()).isEqualTo(640);
        assertThat(decoded.getHeight()).isEqualTo(360);
    }

    @Test
    void 坏字节返回null由存储层回退原图() {
        assertThat(ImageWebpTranscoder.transcode("x.png", new byte[]{1, 2, 3})).isNull();
    }
}
