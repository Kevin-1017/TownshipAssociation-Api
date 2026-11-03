package com.tsa.api.service.impl;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Iterator;

/**
 * 上传图片统一转 WebP 落盘（长边 ≤{@value #MAX_EDGE}，q{@value #QUALITY}）。
 *
 * <p>为什么在存储层做：单机出口带宽是全站图片的回源瓶颈，原图 PNG 动辄 1MB；
 * 转码收口在 {@link LocalFileStorageServiceImpl#upload} 一处，
 * 四个上传端点（通用/活动封面/点位截图/社区配图）零改动即全部生效。
 *
 * <p>回退原则——转码是优化不是闸口：解不出图 / 编码器缺席 / 编码异常时返回 null，
 * 存储层按原字节原扩展名落盘，上传成功率不因压缩链路受损。
 */
@Slf4j
final class ImageWebpTranscoder {

    /** 长边上限：站内最大展示宽度约 900px，1080 留足高倍屏余量 */
    private static final int MAX_EDGE = 1080;

    /** 有损质量：q78 下截图类图片视觉上仍干净，体积约为 PNG 原图的 3%~15% */
    private static final float QUALITY = 0.78f;

    /**
     * 转码结果。
     *
     * @param objectKey 换好 .webp 扩展名的对象名（uuid 不变，只换后缀）
     * @param data      WebP 字节
     */
    record Webp(String objectKey, byte[] data) {
    }

    /**
     * @return 转码产物；入参不是可解码的图片、或编码链路任何一步失败时返回 null（调用方存原图）
     */
    static Webp transcode(String objectKey, byte[] raw) {
        BufferedImage src = decode(raw);
        if (src == null) {
            return null;
        }
        BufferedImage img = downscale(src);
        byte[] encoded = encodeWebp(img, objectKey);
        return encoded == null ? null : new Webp(toWebpKey(objectKey), encoded);
    }

    private static BufferedImage decode(byte[] raw) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(raw));
            if (img == null) {
                log.debug("ImageIO 无匹配解码器，按原图落盘");
            }
            return img;
        } catch (Exception e) {
            log.debug("图片解码失败，按原图落盘", e);
            return null;
        }
    }

    /** 等比缩到长边 ≤1080；够小则原样返回。逐步对半缩再收尾，避免一步双线性在大缩放比下发糊 */
    private static BufferedImage downscale(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        double k = (double) MAX_EDGE / Math.max(w, h);
        if (k >= 1.0) {
            return src;
        }
        int tw = Math.max(1, (int) Math.round(w * k));
        int th = Math.max(1, (int) Math.round(h * k));
        BufferedImage cur = src;
        while (cur.getWidth() / 2 > tw && cur.getHeight() / 2 > th) {
            cur = scaleTo(cur, Math.max(tw, cur.getWidth() / 2), Math.max(th, cur.getHeight() / 2));
        }
        return scaleTo(cur, tw, th);
    }

    private static BufferedImage scaleTo(BufferedImage src, int w, int h) {
        int type = src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage out = new BufferedImage(w, h, type);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g.drawImage(src, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    /** @return WebP 字节；编码器缺席或写盘失败返回 null */
    private static byte[] encodeWebp(BufferedImage img, String objectKey) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
        if (!writers.hasNext()) {
            log.warn("classpath 上没有 WebP 编码器（webp-imageio 依赖缺失？），按原图落盘: {}", objectKey);
            return null;
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(bos)) {
            writer.setOutput(out);
            ImageWriteParam param = writer.getDefaultWriteParam();
            try {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                // WebP 的 WriteParam 要求先选压缩类型再谈质量，否则 setCompressionQuality 抛
                // 「No compression type set!」——按名挑「lossy」，挑不到就吃默认
                String lossy = Arrays.stream(param.getCompressionTypes())
                        .filter(t -> t.toLowerCase(java.util.Locale.ROOT).contains("lossy"))
                        .findFirst().orElse(null);
                if (lossy != null) {
                    param.setCompressionType(lossy);
                }
                param.setCompressionQuality(QUALITY);
            } catch (RuntimeException ignored) {
                // 编码器不接这套标准参数就整体退回默认参数（半设不发的 EXPLICIT param 反而会让 write 抛）
                param = writer.getDefaultWriteParam();
            }
            writer.write(null, new IIOImage(img, null, null), param);
            out.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            log.warn("WebP 编码失败，按原图落盘: {}", objectKey, e);
            return null;
        } finally {
            writer.dispose();
        }
    }

    private static String toWebpKey(String objectKey) {
        int dot = objectKey.lastIndexOf('.');
        return (dot < 0 ? objectKey : objectKey.substring(0, dot)) + ".webp";
    }

    private ImageWebpTranscoder() {
    }
}
