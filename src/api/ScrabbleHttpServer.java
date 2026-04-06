package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import controller.ControleurPartie;
import controller.PlacementCommande;
import model.Direction;
import model.ResultatTour;
import service.ServiceDictionnaireToujoursValide;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

public final class ScrabbleHttpServer {
    private final ControleurPartie controleur;
    private final HttpServer server;

    public ScrabbleHttpServer(int port) throws IOException {
        this.controleur = new ControleurPartie(new ServiceDictionnaireToujoursValide());
        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/health", new JsonHandler(this::health));
        server.createContext("/api/game/state", new JsonHandler(this::etat));
        server.createContext("/api/game/start", new JsonHandler(this::demarrer));
        server.createContext("/api/game/reset", new JsonHandler(this::reinitialiser));
        server.createContext("/api/game/play", new JsonHandler(this::jouer));
        server.createContext("/api/game/pass", new JsonHandler(this::passer));
        server.createContext("/api/game/exchange", new JsonHandler(this::echanger));

        server.setExecutor(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
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
        ensureMethod(exchange, "GET");
        return Response.ok(controleur.exporterEtat());
    }

    private Response demarrer(HttpExchange exchange, Object body) {
        ensureMethod(exchange, "POST");
        Map<String, Object> map = asObject(body);
        List<String> noms = asStringList(map.get("playerNames"));
        controleur.nouvellePartie(noms);
        return Response.ok(controleur.exporterEtat());
    }

    private Response reinitialiser(HttpExchange exchange, Object body) {
        ensureMethod(exchange, "POST");
        controleur.reinitialiser();
        return Response.ok(controleur.exporterEtat());
    }

    private Response jouer(HttpExchange exchange, Object body) {
        ensureMethod(exchange, "POST");

        Map<String, Object> map = asObject(body);

        Direction direction = parseDirection(map.get("direction"));

        List<PlacementCommande> placements = new ArrayList<>();
        for (Object item : asList(map.get("placements"))) {
            Map<String, Object> p = asObject(item);

            String tileId = asString(p.get("tileId"));
            int row = asInt(p.get("row"));
            int col = asInt(p.get("col"));

            Character joker = null;
            Object jokerObj = p.get("jokerFace");
            if (jokerObj instanceof String s && !s.isBlank()) {
                joker = Character.toUpperCase(s.charAt(0));
            }

            placements.add(new PlacementCommande(tileId, row, col, joker));
        }

        ResultatTour resultat = controleur.jouerCoup(placements, direction);

        Map<String, Object> response = new LinkedHashMap<>(controleur.exporterEtat());
        response.put("action", Map.of(
                "points", resultat.points(),
                "words", resultat.mots(),
                "message", resultat.message(),
                "finished", resultat.partieTerminee()
        ));

        return Response.ok(response);
    }

    private Response passer(HttpExchange exchange, Object body) {
        ensureMethod(exchange, "POST");

        String message = controleur.passerTour();

        Map<String, Object> response = new LinkedHashMap<>(controleur.exporterEtat());
        response.put("action", Map.of("message", message));

        return Response.ok(response);
    }

    private Response echanger(HttpExchange exchange, Object body) {
        ensureMethod(exchange, "POST");

        Map<String, Object> map = asObject(body);
        String message = controleur.echangerTuiles(asStringList(map.get("tileIds")));

        Map<String, Object> response = new LinkedHashMap<>(controleur.exporterEtat());
        response.put("action", Map.of("message", message));

        return Response.ok(response);
    }

    private static void ensureMethod(HttpExchange exchange, String method) {
        if (!method.equalsIgnoreCase(exchange.getRequestMethod())) {
            throw new IllegalArgumentException("Méthode non autorisée");
        }
    }

    private static Direction parseDirection(Object value) {
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Direction.valueOf(s.trim().toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException("Direction invalide");
            }
        }
        return null;
    }

    private static Map<String, Object> asObject(Object body) {
        if (body instanceof Map<?, ?> map) {
            Map<String, Object> res = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                res.put(String.valueOf(e.getKey()), e.getValue());
            }
            return res;
        }
        return Map.of();
    }

    private static List<Object> asList(Object value) {
        if (value instanceof List<?> l) {
            return new ArrayList<>(l);
        }
        return List.of();
    }

    private static List<String> asStringList(Object value) {
        List<String> res = new ArrayList<>();
        for (Object o : asList(value)) {
            res.add(asString(o));
        }
        return res;
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int asInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            throw new IllegalArgumentException("Entier invalide");
        }
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
            addHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            try {
                Object body = null;

                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String text = read(exchange.getRequestBody());
                    if (!text.isBlank()) {
                        body = Json.parse(text);
                    }
                }

                Response response = route.handle(exchange, body);
                write(exchange, response.statusCode, response.body);

            } catch (IllegalArgumentException | IllegalStateException e) {
                write(exchange, 400, Map.of("error", e.getMessage()));
            } catch (Exception e) {
                write(exchange, 500, Map.of("error", Optional.ofNullable(e.getMessage()).orElse("Erreur interne")));
            }
        }

        private static String read(InputStream is) throws IOException {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        private static void addHeaders(HttpExchange exchange) {
            var h = exchange.getResponseHeaders();
            h.add("Access-Control-Allow-Origin", "*");
            h.add("Access-Control-Allow-Headers", "Content-Type");
            h.add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            h.add("Content-Type", "application/json; charset=utf-8");
        }

        private static void write(HttpExchange exchange, int code, Object body) throws IOException {
            byte[] data = Json.stringify(body).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }
    }

    private record Response(int statusCode, Object body) {
        static Response ok(Object body) {
            return new Response(200, body);
        }

        static Response error(int code, String message) {
            return new Response(code, Map.of("error", message));
        }
    }
}