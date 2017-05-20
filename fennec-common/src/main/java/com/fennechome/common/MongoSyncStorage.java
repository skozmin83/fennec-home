package com.fennechome.common;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.configuration2.Configuration;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

public class MongoSyncStorage implements AutoCloseable {
    private final Map<String, MongoCollection<Document>> collections = new HashMap<>();
    private final MongoClient mongoClient;
    private final MongoDatabase database;

    public MongoSyncStorage(Configuration config) {
        mongoClient = new MongoClient(new MongoClientURI(config.getString("fennec.mongo.connection-string")));
        database = mongoClient.getDatabase(config.getString("fennec.mongo.database-name"));
    }

    public void store(Document deviceInfo, String collection) {
        getCollection(collection).insertOne(deviceInfo);
    }

    public void load(String collection, Loader loader) {
        loader.load(getCollection(collection));
    }

    private MongoCollection<Document> getCollection(String store) {
        return collections.computeIfAbsent(store, database::getCollection);
    }

    @Override
    public void close() throws Exception {
        mongoClient.close();
    }

    public interface Loader {
        void load(MongoCollection<Document> collection);
    }
}