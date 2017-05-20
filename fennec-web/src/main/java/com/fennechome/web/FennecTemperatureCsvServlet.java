package com.fennechome.web;

import com.fennechome.common.FennecException;
import com.fennechome.common.MongoSyncStorage;
import com.mongodb.Block;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.mongodb.client.model.Filters.and;

public class FennecTemperatureCsvServlet extends HttpServlet {
    private final Logger                     logger             = LoggerFactory.getLogger(getClass());
    private final ThreadLocal<LineGenerator> lineGeneratorLocal = ThreadLocal.withInitial(LineGenerator::new);
    private final MongoSyncStorage storage;
    private final String           collection;

    public FennecTemperatureCsvServlet(MongoSyncStorage storage, String collection) {
        this.storage = storage;
        this.collection = collection;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String[]> map = req.getParameterMap();
        List<Bson> filters = new ArrayList<>(map.size());
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            filters.add(Filters.regex(entry.getKey(), entry.getValue()[0])); // todo optimize
        }
        resp.setContentType("text/csv");

        try (ServletOutputStream outputStream = resp.getOutputStream()) {
            LineGenerator lineGenerator = lineGeneratorLocal.get();
            lineGenerator.setOutputStream(outputStream);

            Date from = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
            filters.add(Filters.gt("time", from));
            logger.info("Request: " + filters);
            storage.load(collection, collection -> collection
                    .find(and(filters))
                    .sort(Sorts.ascending("time", "sid"))
                    .forEach(lineGenerator));
            outputStream.flush();
        } catch (Exception e) {
            throw new FennecException("Unable to reply with csv. ", e);
        }
    }

    private static class LineGenerator implements Block<Document> {
        private final StringBuilder sb = new StringBuilder();
        private final DateFormat   df;
        private       OutputStream outputStream;

        public LineGenerator() {
            TimeZone tz = TimeZone.getTimeZone("America/New_York");
//            Quoted "Z" to indicate UTC, no timezone offset
//            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
            df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            df.setTimeZone(tz);
        }

        public void setOutputStream(OutputStream outputStream) {
            this.outputStream = outputStream;
            try {
                outputStream.write("sid,t,ts\n".getBytes());
            } catch (IOException e) {
                throw new FennecException("Unable to write to output. ", e);
            }
        }

        @Override
        public void apply(Document document) {
            try {
                sb.setLength(0);
                long ts = document.getLong("ts");
                String sid = document.getString("sid");
                if (sid != null) {
//                    String ts6081 = df.format(ts);
                    sb.append(sid)
                      .append(",")
                      .append(document.get("t"))
                      .append(",")
                      .append(ts)
                      .append("\n");
                    outputStream.write(sb.toString().getBytes());
                }
            } catch (IOException e) {
                throw new FennecException("Unable to write to output. ", e);
            }
        }
    }
}
