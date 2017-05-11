package com.fennechome.mqtt;

import io.netty.buffer.ByteBuf;

import java.io.UnsupportedEncodingException;

public class JsonMongoMsgParser {
    public static String decodeString(ByteBuf in) throws UnsupportedEncodingException {
        return new String(readFixedLengthContent(in), "UTF-8");
    }

    /**
     * Read a byte array from the buffer, use two bytes as length information followed by length bytes.
     * */
    public static byte[] readFixedLengthContent(ByteBuf in) throws UnsupportedEncodingException {
        if (in.readableBytes() < 2) {
            return new byte[]{};
        }
        int strLen = in.readableBytes();
        byte[] strRaw = new byte[strLen];
        in.readBytes(strRaw);
        return strRaw;
    }
}
