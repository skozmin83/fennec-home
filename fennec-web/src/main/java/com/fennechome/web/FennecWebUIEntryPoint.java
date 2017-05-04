package com.fennechome.web;

public class FennecWebUIEntryPoint {
    public static void main(String[] args) {
        String resourceBase = args.length > 1 ? args[0] : "./fennec-web/src/main/resources/webroot";
        FennecWebServer server = new FennecWebServer(resourceBase);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        server.join();
    }
}
