package com.fennechome.server;

import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import org.apache.commons.configuration2.Configuration;
import org.bson.Document;

import java.util.Date;

public class MongoStorage implements AutoCloseable {
    private final MongoCollection<Document> collection;
    private final MongoClient mongoClient;

    public MongoStorage(Configuration config) {
        mongoClient = MongoClients.create(config.getString("fennec.mongo.connection-string"));
        MongoDatabase database = mongoClient.getDatabase(config.getString("fennec.mongo.database-name"));
        collection = database.getCollection(config.getString("fennec.mongo.collection"));
    }

    public void store(Document deviceInfo, String topicName) {
        collection.insertOne(deviceInfo, (result, t) -> {});
    }

    @Override
    public void close() throws Exception {
        mongoClient.close();
    }
}