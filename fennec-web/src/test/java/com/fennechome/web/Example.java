package com.fennechome.web;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.TimeZone;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;

public class Example {
    public static void main(String[] args) {
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase db = mongoClient.getDatabase("testdb");
        MongoCollection<Document> collection = db.getCollection("zone-events");

        TimeZone tz = TimeZone.getTimeZone("America/New_York");
//            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        StringBuilder sb = new StringBuilder();

        Date from = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
        System.out.println("Start from [" + from + "] elements. ");
        final int[] counter = {0};
        collection
//                .find()
//                .find(and(Filters.regex("sid", "dht22-top"), Filters.regex("topic", "A0:20:A6:16:A6:34")))
                .find(and(
//                        Filters.regex("sid", "(dht22-top|dht22-bottom)")
                        Filters.regex("id", "5C:CF:7F:34:37:E0")
//                        ,Filters.regex("id", "5C:CF:7F:34:37:E0")
                      ,  Filters.gt("time", from)
                )).sort(Sorts.ascending("ts", "sid"))
                .forEach((Block<Document>) document -> {
                    counter[0]++;
                    sb.setLength(0);
                    Date ts = (Date) document.get("time");
                    String sid = (String) document.get("sid");
                    if (ts != null
//                            && sid != null
                            ) {
                        String ts6081 = df.format(ts);
                        sb.append(sid).append(",").append(document.get("t")).append(",").append(ts6081).append("\n");
                        System.out.print(sb);
                    }
                });
        System.out.println("Loaded [" + counter[0] + "] elements. ");
    }
}