package view;

import api.ScrabbleHttpServer;

public final class Main {
    private Main() { }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        ScrabbleHttpServer server = new ScrabbleHttpServer(port);
        server.start();
        System.out.println("Backend Scrabble v3 lancé sur http://localhost:" + port);
    }
}
