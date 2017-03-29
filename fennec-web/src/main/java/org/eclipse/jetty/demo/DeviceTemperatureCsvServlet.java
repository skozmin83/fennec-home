package org.eclipse.jetty.demo;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

import com.mongodb.*;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Filters;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;

import com.mongodb.client.model.Sorts;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import org.bson.Document;

public class DeviceTemperatureCsvServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/csv");
//        resp.setHeader("Content-Disposition", "attachment; filename=\"userDirectory.csv\"");
        try {
            OutputStream outputStream = resp.getOutputStream();
            outputStream.write("id,t,ts\n".getBytes());
            MongoClient mongoClient = new MongoClient("raspberrypi", 27017);
            MongoDatabase db = mongoClient.getDatabase("mydb");
            MongoCollection<Document> mycoll = db.getCollection("mycoll");

            TimeZone tz = TimeZone.getTimeZone("UTC");
//            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Quoted "Z" to indicate UTC, no timezone offset
            df.setTimeZone(tz);
            StringBuilder sb = new StringBuilder();

            mycoll.find().forEach(new Block<Document>() {
                @Override
                public void apply(Document document) {
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
