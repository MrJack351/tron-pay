package com.tron.pay.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.IoUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.extra.qrcode.QrCodeUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonUtils {
    public static String convertMillisecondsToDateString(String milliseconds) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return formatter.format(new Date(Long.parseLong(milliseconds)));
    }

    public static boolean isMobileDevice(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && !userAgent.isEmpty()) {
            userAgent = userAgent.toLowerCase();
            if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
                return true; // 访问设备为手机
            }
        }
        return false; // 访问设备为PC
    }

    /**
     * 生成二维码图片的字节数组
     *
     * @param content 二维码内容
     * @param width   二维码宽度
     * @param height  二维码高度
     * @return 二维码图片的字节数组
     */
    public static byte[] generateQRCode(String content, int width, int height){
        String format = "png";
        BitMatrix bitMatrix = null;
        try {
            bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height);
        } catch (WriterException e) {
            throw new RuntimeException(e);
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, format, baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    /**
     * 动态设置前端样式
     * @param status
     * @return
     */
    public static String getStatusClass(String status) {
        switch (status) {
            case "success":
                return "badge badge-pill badge-success";
            case "wait":
                return "badge badge-pill badge-secondary";
            case "failure":
                return "badge badge-pill badge-danger";
            default:
                return "";
        }
    }

    public static String getStatusValue(String status) {
        switch (status) {
            case "success":
                return "已支付";
            case "wait":
                return "待支付";
            case "failure":
                return "badge badge-pill badge-danger";
            default:
                return "";
        }
    }

    public static String verifySign(String amount, String coinType, String token, String merchantKey) {
        // 拼接待签名字符串（金额、币种、商户端订单号、通知地址、商户私钥）
        String md5Str = amount + coinType + token + merchantKey;
        // 计算签名
        // 验证签名是否正确
        return MD5.create().digestHex(md5Str);
    }

}
