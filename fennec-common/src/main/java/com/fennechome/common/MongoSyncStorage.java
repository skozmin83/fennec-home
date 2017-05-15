package com.fennechome.common;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.configuration2.Configuration;
import org.bson.Document;

public class MongoSyncStorage implements AutoCloseable {
    private final MongoCollection<Document> collection;
    private final MongoClient mongoClient;

    public MongoSyncStorage(Configuration config) {
        mongoClient = new MongoClient(new MongoClientURI(config.getString("fennec.mongo.connection-string")));
        MongoDatabase db = mongoClient.getDatabase(config.getString("fennec.mongo.database-name"));
        collection = db.getCollection(config.getString("fennec.mongo.collection"));
    }

    public void store(Document deviceInfo, String topicName) {
        collection.insertOne(deviceInfo);
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