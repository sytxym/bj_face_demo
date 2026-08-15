package com.aeye.face.api.gateway;

/**
 * 十六进制编解码工具（网关签名/SM2 密钥均为 hex 字符串）。
 * 独立实现，不复用 {@code com.aeye.face.uitls.SMUtil}，保持本包对国密算法自包含、不依赖闭源 smsdk.jar。
 */
final class HexCodec {

    private static final char[] HEX_LOWER = "0123456789abcdef".toCharArray();

    private HexCodec() {
    }

    static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX_LOWER[v >>> 4];
            out[i * 2 + 1] = HEX_LOWER[v & 0x0F];
        }
        return new String(out);
    }

    static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return new byte[0];
        }
        String h = hex;
        if (h.length() % 2 != 0) {
            h = "0" + h;
        }
        int len = h.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(h.charAt(i), 16);
            int lo = Character.digit(h.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("非法十六进制字符串: " + hex);
            }
            out[i / 2] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
