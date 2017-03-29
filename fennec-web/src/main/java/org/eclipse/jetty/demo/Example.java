package org.eclipse.jetty.demo;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.Consumer;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * Java MongoDB : Query document
 *
 * @author mkyong
 *
 */
public class Example {

    public static void insertDummyDocuments(DBCollection collection) {

        List<DBObject> list = new ArrayList<DBObject>();

        Calendar cal = Calendar.getInstance();

        for (int i = 1; i <= 5; i++) {

            BasicDBObject data = new BasicDBObject();
            data.append("number", i);
            data.append("name", "mkyong-" + i);
            // data.append("date", cal.getTime());

            // +1 day
            cal.add(Calendar.DATE, 1);

            list.add(data);

        }

        collection.insert(list);

    }

    public static void main(String[] args) {

        MongoClient mongoClient = new MongoClient( "raspberrypi" , 27017 );
        MongoDatabase db = mongoClient.getDatabase("mydb");
        MongoCollection<Document> mycoll = db.getCollection("mycoll");
//        List<String> dbs = mongoClient.getDatabaseNames();
        /*for(String db : dbs)
        {
            System.out.println(db);
        }*/
        mycoll.find().forEach(new Block<Document>() {
            @Override
            public void apply(Document document) {
                System.out.println(document);
            }
        });
    }
}