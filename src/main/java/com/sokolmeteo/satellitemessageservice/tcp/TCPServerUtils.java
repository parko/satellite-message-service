package com.sokolmeteo.satellitemessageservice.tcp;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class TCPServerUtils {
    public static byte[] readBytes(DataInputStream inputStream, int maxSize) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] bytes = new byte[maxSize];
        int n;
        n = inputStream.read(bytes);
        if (n == -1) return null;
        outputStream.write(bytes, 0, n);
        return outputStream.toByteArray();
    }

    public static int byteArrayToInt(byte[] bytes, int start, int length) {
        int data = 0;
        if ((bytes[start] & 0x80) != 0) {
            data = Integer.MAX_VALUE;
        }
        for (int i = 0; i < length; i++) {
            data = (data << 8) + (bytes[start++] & 255);
        }
        return data;
    }

    public static long byteArrayToLong(byte[] bytes, int start, int length) {
        long result = 0;
        for (int i = 0; i < length; i++) {
            result = result << 8;
            result = result + (bytes[start++] & 0xff);
        }
        return result;
    }
}
