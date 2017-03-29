package com.fennechome.server;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MongoStorage implements IDeviceInfoStorage {
    private final MongoCollection<Document> collection;
    public MongoStorage() {
// Use a Connection String
        MongoClient mongoClient = MongoClients.create("mongodb://localhost");
        MongoDatabase database = mongoClient.getDatabase("mydb");
        collection = database.getCollection("mycoll");
    }

    public void store(Document deviceInfo, String topicName) {
        deviceInfo.append("topic", topicName);
        deviceInfo.append("ts", new Date());
        collection.insertOne(deviceInfo, new SingleResultCallback<Void>() {
            @Override
            public void onResult(final Void result, final Throwable t) { }
        });
        /*collection.insert(deviceInfo, new SingleResultCallback<Void>() {
            @Override
            public void onResult(final Void result, final Throwable t) {
                System.out.println("Inserted!");
            }
        })*/
    }

    public void store(DeviceInfo deviceInfo) {
// insert a document
        Document document = new Document("x", 1);
        collection.insertOne(document, new SingleResultCallback<Void>() {
            @Override
            public void onResult(final Void result, final Throwable t) {
                System.out.println("Inserted!");
            }
        });

        document.append("x", 2).append("y", 3);

// replace a document
        collection.replaceOne(Filters.eq("_id", document.get("_id")), document,
            new SingleResultCallback<UpdateResult>() {
                @Override
                public void onResult(final UpdateResult result, final Throwable t) {
                    System.out.println(result.getModifiedCount());
                }
            });

// find documents
        collection.find().into(new ArrayList<Document>(),
            new SingleResultCallback<List<Document>>() {
                @Override
                public void onResult(final List<Document> result, final Throwable t) {
                    System.out.println("Found Documents: #" + result.size());
                }
            });
    }
}
/*
class MongoDBInsertDataExample
{
    public static void main(String[] args) throws UnknownHostException
    {
        MongoClient mongo = new MongoClient("localhost", 27017);
        MongoDatabase db = mongo.getDatabase("howtodoinjava");
        MongoCollection collection = db.getCollection("users");

        ///Delete All documents before running example again
        WriteResult result = collection.remove(new BasicDBObject());
        System.out.println(result.toString());

        basicDBObject_Example(collection);

        basicDBObjectBuilder_Example(collection);

        hashMap_Example(collection);

        parseJSON_Example(collection);

        DBCursor cursor = collection.find();
        while(cursor.hasNext()) {
            System.out.println(cursor.next());
        }
    }

    private static void basicDBObject_Example(DBCollection collection){
        BasicDBObject document = new BasicDBObject();
        document.put("name", "lokesh");
        document.put("website", "howtodoinjava.com");

        BasicDBObject documentDetail = new BasicDBObject();
        documentDetail.put("addressLine1", "Sweet Home");
        documentDetail.put("addressLine2", "Karol Bagh");
        documentDetail.put("addressLine3", "New Delhi, India");

        document.put("address", documentDetail);

        collection.insert(document);
    }

    private static void basicDBObjectBuilder_Example(DBCollection collection){
        BasicDBObjectBuilder documentBuilder = BasicDBObjectBuilder.start()
            .add("name", "lokesh")
            .add("website", "howtodoinjava.com");

        BasicDBObjectBuilder documentBuilderDetail = BasicDBObjectBuilder.start()
            .add("addressLine1", "Some address")
            .add("addressLine2", "Karol Bagh")
            .add("addressLine3", "New Delhi, India");

        documentBuilder.add("address", documentBuilderDetail.get());

        collection.insert(documentBuilder.get());
    }

    private static void hashMap_Example(DBCollection collection){
        Map<String, Object> documentMap = new HashMap<String, Object>();
        documentMap.put("name", "lokesh");
        documentMap.put("website", "howtodoinjava.com");

        Map<String, Object> documentMapDetail = new HashMap<String, Object>();
        documentMapDetail.put("addressLine1", "Some address");
        documentMapDetail.put("addressLine2", "Karol Bagh");
        documentMapDetail.put("addressLine3", "New Delhi, India");

        documentMap.put("address", documentMapDetail);

        collection.insert(new BasicDBObject(documentMap));
    }

    private static void parseJSON_Example(DBCollection collection){
        String json = "{ 'name' : 'lokesh' , " +
            "'website' : 'howtodoinjava.com' , " +
            "'address' : { 'addressLine1' : 'Some address' , " +
            "'addressLine2' : 'Karol Bagh' , " +
            "'addressLine3' : 'New Delhi, India'}" +
            "}";

        DBObject dbObject = (DBObject)JSON.parse(json);

        collection.insert(dbObject);
    }

}*/
