package com.fennechome.server;

import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.netty.buffer.ByteBuf;
import org.bson.Document;

import java.io.UnsupportedEncodingException;

/**
 * Saves all the information published by devices to a file
 */
class DeviceInfoSaverListener extends AbstractInterceptHandler {
    private final MongoStorage st = new MongoStorage();
    private final IMsgParser parser = new JsonMsgParser();

    @Override
    public String getID() {
        return getClass().getSimpleName();
    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
        String topicName = msg.getTopicName();
        String payload = null;
        try {
            payload = JsonMongoMsgParser.decodeString(msg.getPayload());
            Document dbObject = Document.parse(payload);
            st.store(dbObject, topicName);
        } catch (Exception e) {
            System.out.println("topicName = " + topicName + ", msg: " + payload);
            e.printStackTrace();
        }
//        try {
//            System.out.println("Received on topic: " + msg.getTopicName() + " content: " + decodeString(msg.getPayload()));
//            st.store(parser.parse(msg));
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * Load a string from the given buffer, reading first the two bytes of len
     * and then the UTF-8 bytes of the string.
     *
     * @return the decoded string or null if NEED_DATA
     */
    private String decodeString(ByteBuf in) throws UnsupportedEncodingException {
        return new String(readFixedLengthContent(in), "UTF-8");
    }

    /**
     * Read a byte array from the buffer, use two bytes as length information followed by length bytes.
     * */
    private byte[] readFixedLengthContent(ByteBuf in) throws UnsupportedEncodingException {
        if (in.readableBytes() < 2) {
            return new byte[]{};
        }
        int strLen = in.readableBytes();
//        if (in.readableBytes() < strLen) {
//            return null;
//        }
        byte[] strRaw = new byte[strLen];
        in.readBytes(strRaw);

        return strRaw;
    }
}
