package org.eclipse.jetty.demo;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;

public class Example {
    public static void main(String[] args) {
        MongoClient mongoClient = new MongoClient("raspberrypi", 27017);
        MongoDatabase db = mongoClient.getDatabase("mydb");
        MongoCollection<Document> collection = db.getCollection("mycoll");

        TimeZone tz = TimeZone.getTimeZone("America/New_York");
//            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        StringBuilder sb = new StringBuilder();

        collection
//                .find()
                .find(and(Filters.regex("sid", "dht22-top"), Filters.regex("topic", "A0:20:A6:16:A6:34")))
                .sort(Sorts.ascending("ts", "sid"))
                .forEach((Block<Document>) document -> {
                    sb.setLength(0);
                    Date ts = (Date) document.get("ts");
                    String sid = (String) document.get("sid");
                    if (ts != null && sid != null) {
                        String ts6081 = df.format(ts);
                        sb.append(sid).append(",").append(document.get("t")).append(",").append(ts6081).append("\n");
                        System.out.print(sb);
                    }
                });
    }
}