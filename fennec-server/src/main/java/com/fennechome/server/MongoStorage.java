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

public class MongoStorage implements AutoCloseable {
    private final MongoCollection<Document> collection;
    private final MongoClient mongoClient;

    public MongoStorage() {
        mongoClient = MongoClients.create("mongodb://localhost");
        MongoDatabase database = mongoClient.getDatabase("mydb");
        collection = database.getCollection("mycoll");
    }

    public void store(Document deviceInfo, String topicName) {
        deviceInfo.append("topic", topicName);
        deviceInfo.append("ts", new Date());
        collection.insertOne(deviceInfo, new SingleResultCallback<Void>() {
            @Override
            public void onResult(final Void result, final Throwable t) {
            }
        });
    }

    @Override
    public void close() throws Exception {
        mongoClient.close();
    }
}