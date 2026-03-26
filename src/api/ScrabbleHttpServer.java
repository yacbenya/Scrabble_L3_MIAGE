package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import controller.ControleurPartie;
import controller.PlacementCommande;
import model.Direction;
import model.ResultatTour;
import service.ServiceDictionnaireToujoursValide;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public final class ScrabbleHttpServer {
    private final ControleurPartie controleur;
    private final HttpServer server;

    public ScrabbleHttpServer(int port) throws IOException {
        this.controleur = new ControleurPartie(new ServiceDictionnaireToujoursValide());
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/api/health", new JsonHandler(this::health));
        this.server.createContext("/api/game/state", new JsonHandler(this::etat));
        this.server.createContext("/api/game/start", new JsonHandler(this::demarrer));
        this.server.createContext("/api/game/reset", new JsonHandler(this::reinitialiser));
        this.server.createContext("/api/game/play", new JsonHandler(this::jouer));
        this.server.createContext("/api/game/pass", new JsonHandler(this::passer));
        this.server.createContext("/api/game/exchange", new JsonHandler(this::echanger));
        this.server.setExecutor(Executors.newCachedThreadPool());
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private Response health(HttpExchange exchange, Object body) {
        return Response.ok(Map.of("status", "ok"));
    }

    private Response etat(HttpExchange exchange, Object body) {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            return Response.error(405, "Méthode non autorisée.");
        }
        return Response.ok(controleur.exporterEtat());
    }

    private Response demarrer(HttpExchange exchange, Object body) {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            return Response.error(405, "Méthode non autorisée.");
        }
        Map<String, Object> map = asObject(body);
        List<String> noms = asStringList(map.get("playerNames"));
        controleur.nouvellePartie(noms);
        return Response.ok(controleur.exporterEtat());
    }


    private Response reinitialiser(HttpExchange exchange, Object body) {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            return Response.error(405, "Méthode non autorisée.");
        }
        controleur.reinitialiser();
        return Response.ok(controleur.exporterEtat());
    }

    private Response jouer(HttpExchange exchange, Object body) {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            return Response.error(405, "Méthode non autorisée.");
        }
        Map<String, Object> map = asObject(body);
        Direction direction = null;
        Object directionObject = map.get("direction");
        if (directionObject instanceof String texte && !texte.isBlank()) {
            direction = Direction.valueOf(texte.trim().toUpperCase());
        }

        List<PlacementCommande> placements = new ArrayList<>();
        for (Object item : asList(map.get("placements"))) {
            Map<String, Object> placement = asObject(item);
            String tileId = asString(placement.get("tileId"));
            int ligne = asInt(placement.get("row"));
            int colonne = asInt(placement.get("col"));
            Character faceJoker = null;
            Object jokerObject = placement.get("jokerFace");
            if (jokerObject instanceof String joker && !joker.isBlank()) {
                faceJoker = Character.toUpperCase(joker.charAt(0));
            }
            placements.add(new PlacementCommande(tileId, ligne, colonne, faceJoker));
        }

        ResultatTour resultat = controleur.jouerCoup(placements, direction);
        Map<String, Object> reponse = new LinkedHashMap<>(controleur.exporterEtat());
        reponse.put("action", Map.of(
                "points", resultat.points(),
                "words", resultat.mots(),
                "message", resultat.message(),
                "finished", resultat.partieTerminee()
        ));
        return Response.ok(reponse);
    }

    private Response passer(HttpExchange exchange, Object body) {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            return Response.error(405, "Méthode non autorisée.");
        }
        String message = controleur.passerTour();
        Map<String, Object> reponse = new LinkedHashMap<>(controleur.exporterEtat());
        reponse.put("action", Map.of("message", message));
        return Response.ok(reponse);
    }

    private Response echanger(HttpExchange exchange, Object body) {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            return Response.error(405, "Méthode non autorisée.");
        }
        Map<String, Object> map = asObject(body);
        String message = controleur.echangerTuiles(asStringList(map.get("tileIds")));
        Map<String, Object> reponse = new LinkedHashMap<>(controleur.exporterEtat());
        reponse.put("action", Map.of("message", message));
        return Response.ok(reponse);
    }

    private static Map<String, Object> asObject(Object body) {
        if (body instanceof Map<?, ?> map) {
            Map<String, Object> resultat = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                resultat.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return resultat;
        }
        return Map.of();
    }

    private static List<Object> asList(Object value) {
        if (value instanceof List<?> liste) {
            return new ArrayList<>(liste);
        }
        return List.of();
    }

    private static List<String> asStringList(Object value) {
        List<String> resultat = new ArrayList<>();
        for (Object item : asList(value)) {
            resultat.add(asString(item));
        }
        return resultat;
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int asInt(Object value) {
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(String.valueOf(value));
    }

    private interface Route {
        Response handle(HttpExchange exchange, Object body) throws Exception;
    }

    private static final class JsonHandler implements HttpHandler {
        private final Route route;

        private JsonHandler(Route route) {
            this.route = route;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            try {
                Object body = null;
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String texte = readBody(exchange.getRequestBody());
                    if (!texte.isBlank()) {
                        body = Json.parse(texte);
                    }
                }

                Response response = route.handle(exchange, body);
                writeJson(exchange, response.statusCode, response.body);
            } catch (IllegalArgumentException | IllegalStateException e) {
                writeJson(exchange, 400, Map.of("error", e.getMessage()));
            } catch (Exception e) {
                writeJson(exchange, 500, Map.of("error", e.getMessage() == null ? "Erreur interne." : e.getMessage()));
            }
        }

        private static String readBody(InputStream inputStream) throws IOException {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        private static void addCors(HttpExchange exchange) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        }

        private static void writeJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
            byte[] payload = Json.stringify(body).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, payload.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(payload);
            }
        }
    }

    private record Response(int statusCode, Object body) {
        static Response ok(Object body) {
            return new Response(200, body);
        }

        static Response error(int statusCode, String message) {
            return new Response(statusCode, Map.of("error", message));
        }
    }
}
