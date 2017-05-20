package com.fennechome.common;

import org.bson.Document;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriter;
import org.bson.json.JsonWriterSettings;

public class FasterJsonSerializer {
    private final JsonWriterSettings settings = new JsonWriterSettings(JsonMode.STRICT);
    private final DocumentCodec      codec    = new DocumentCodec();
    private final ReWriter           reWriter = new ReWriter();
    private final JsonWriter         writer   = new JsonWriter(reWriter, settings);
    private final EncoderContext     build    =
            EncoderContext.builder()
                          .isEncodingCollectibleDocument(false)
                          .build();

    public String serialize(Document json) {
        reWriter.clean();
        codec.encode(writer, json, build);
        return writer.getWriter().toString();
    }
}
