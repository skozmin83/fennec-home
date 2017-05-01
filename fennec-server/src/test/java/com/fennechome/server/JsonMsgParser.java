package com.fennechome.server;

import io.moquette.interception.messages.InterceptPublishMessage;
import io.netty.buffer.ByteBuf;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import javax.json.Json;
import javax.json.stream.*;

/**
 * Created by sergey on 3/20/2017.
 */
public class JsonMsgParser {
    public void parse(InterceptPublishMessage msg) {
        String topicName = msg.getTopicName();
        String payload = null;
        try {
            payload = decodeString(msg.getPayload());
            JsonParser parser = Json.createParser(new StringReader(payload));
            // Advance to "address" key
            JsonParser.Event event = parser.next();
//        if(event == JsonParser.Event.KEY_NAME && "address".equals(jr.getString())) {
//            event = jr.next();
//            break;
//        }

            // Output contents of "address" object
            while(event != JsonParser.Event.END_OBJECT) {
                switch(event) {
                    case KEY_NAME: {
                        System.out.print(parser.getString());
                        System.out.print(" = ");
                        break;
                    }
                    case VALUE_FALSE: {
                        System.out.println(false);
                        break;
                    }
                    case VALUE_NULL: {
                        System.out.println("null");
                        break;
                    }
                    case VALUE_NUMBER: {
                        if(parser.isIntegralNumber()) {
                            System.out.println(parser.getInt());
                        } else {
                            System.out.println(parser.getBigDecimal());
                        }
                        break;
                    }
                    case VALUE_STRING: {
                        System.out.println(parser.getString());
                        break;
                    }
                    case VALUE_TRUE: {
                        System.out.println(true);
                        break;
                    }
                    default: {
                    }
                }
                event = parser.next();
            }

//        JsonObject obj = rdr.read();
//        JsonArray results = obj.getJsonArray("data");
//        for (JsonObject result : results.getValuesAs(JsonObject.class)) {
//            System.out.print(result.getJsonObject("from").getString("name"));
//            System.out.print(": ");
//             System.out.println(result.getString("message", ""));
//             System.out.println("-----------");
//         }
//            info.deviceId = topicName.substring(topicName.lastIndexOf("/"));
//        info.temperature = topicName.substring(topicName.lastIndexOf("/"));
        } catch (Exception e) {
            System.out.println("topicName = " + topicName + ", msg: " + payload);
            e.printStackTrace();
        }
    }

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
