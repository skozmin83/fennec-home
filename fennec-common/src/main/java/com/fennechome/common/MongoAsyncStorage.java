package com.fennechome.common;

import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import org.apache.commons.configuration2.Configuration;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

public class MongoAsyncStorage implements AutoCloseable {
    private final Map<String, MongoCollection<Document>> collections = new HashMap<>();
    private final MongoClient   mongoClient;
    private final MongoDatabase database;

    public MongoAsyncStorage(Configuration config) {
        mongoClient = MongoClients.create(config.getString("fennec.mongo.connection-string"));
        database = mongoClient.getDatabase(config.getString("fennec.mongo.database-name"));
    }

    public void store(String store, Document deviceInfo) {
        getCollection(store).insertOne(deviceInfo, (result, t) -> {});
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