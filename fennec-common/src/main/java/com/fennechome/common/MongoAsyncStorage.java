package com.fennechome.common;

import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import org.apache.commons.configuration2.Configuration;
import org.bson.Document;

public class MongoAsyncStorage implements AutoCloseable {
    private final MongoCollection<Document> collection;
    private final MongoClient mongoClient;

    public MongoAsyncStorage(Configuration config) {
        mongoClient = MongoClients.create(config.getString("fennec.mongo.connection-string"));
        MongoDatabase database = mongoClient.getDatabase(config.getString("fennec.mongo.database-name"));
        collection = database.getCollection(config.getString("fennec.mongo.collection"));
    }

    public void store(Document deviceInfo, String topicName) {
        collection.insertOne(deviceInfo, (result, t) -> {});
    }

    public void load(Loader loader) {
        loader.load(collection);
    }

    @Override
    public void close() throws Exception {
        mongoClient.close();
    }

    public interface Loader {
        void load(MongoCollection<Document> collection);
    }
}