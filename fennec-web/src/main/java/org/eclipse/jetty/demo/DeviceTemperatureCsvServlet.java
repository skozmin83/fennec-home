package org.eclipse.jetty.demo;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;

public class DeviceTemperatureCsvServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        System.out.println("req = " + req);
        Map<String, String[]> map = req.getParameterMap();
        List<Bson> filters = new ArrayList<>(map.size());
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            filters.add(Filters.regex(entry.getKey(), entry.getValue()[0])); // todo optimize
        }
        resp.setContentType("text/csv");
//        resp.setHeader("Content-Disposition", "attachment; filename=\"userDirectory.csv\"");
        try {
            OutputStream outputStream = resp.getOutputStream();
            outputStream.write("sid,t,ts\n".getBytes());
            MongoClient mongoClient = new MongoClient("raspberrypi", 27017);
            MongoDatabase db = mongoClient.getDatabase("mydb");
            MongoCollection<Document> collection = db.getCollection("mycoll");

            TimeZone tz = TimeZone.getTimeZone("America/New_York");
//            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Quoted "Z" to indicate UTC, no timezone offset
            df.setTimeZone(tz);
            StringBuilder sb = new StringBuilder();

            Date from = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
//            Bson and = and(eq("sid", "dht22-top"), Filters.regex("topic", "A0:20:A6:16:A6:34"));
            filters.add(Filters.gt("ts", from));
//            filters.add(Filters.gt("aaa", from));
            collection
                    .find(and(filters))
//                    .find(or(eq("sid", "dht22-top"), eq("sid", "dht22-bottom")))
                    .sort(Sorts.ascending("ts", "sid"))
                    .forEach((Block<Document>) document -> {
                        try {
                            sb.setLength(0);
                            Date ts = (Date) document.get("ts");
                            String sid = (String) document.get("sid");
                            if (ts != null && sid != null) {
                                String ts6081 = df.format(ts);
                                sb.append(sid).append(",").append(document.get("t")).append(",").append(ts6081).append("\n");
                                outputStream.write(sb.toString().getBytes());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

//            collection.find(
//                    and(eq("sid", "dht22-bottom"))
//                    and(gte("stars", 2), lt("stars", 5), eq("categories", "Bakery"))
//            ).sort(Sorts.ascending("sid"))
//                    .forEach(printBlock);

//           String outputResult = "id,t,h,ts\n" +
//                   "dht22-yellow,22.1,38.2,2017-03-29 00:00:24\n" +
//                   "dht22-yellow,22.1,37.9,2017-03-29 00:02:26";
//            outputStream.write(outputResult.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
