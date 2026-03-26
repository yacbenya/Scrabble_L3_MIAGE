package api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() { }

    public static Object parse(String texte) {
        if (texte == null) return null;
        Parser parser = new Parser(texte);
        Object valeur = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isEnd()) throw new IllegalArgumentException("JSON invalide.");
        return valeur;
    }

    public static String stringify(Object valeur) {
        StringBuilder sb = new StringBuilder();
        appendValue(sb, valeur);
        return sb.toString();
    }

    private static void appendValue(StringBuilder sb, Object valeur) {
        if (valeur == null) {
            sb.append("null");
            return;
        }
        if (valeur instanceof String texte) {
            sb.append('"').append(escape(texte)).append('"');
            return;
        }
        if (valeur instanceof Number || valeur instanceof Boolean) {
            sb.append(valeur);
            return;
        }
        if (valeur instanceof Map<?, ?> map) {
            sb.append('{');
            boolean premier = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!premier) sb.append(',');
                premier = false;
                appendValue(sb, String.valueOf(entry.getKey()));
                sb.append(':');
                appendValue(sb, entry.getValue());
            }
            sb.append('}');
            return;
        }
        if (valeur instanceof Iterable<?> iterable) {
            sb.append('[');
            boolean premier = true;
            for (Object item : iterable) {
                if (!premier) sb.append(',');
                premier = false;
                appendValue(sb, item);
            }
            sb.append(']');
            return;
        }
        appendValue(sb, String.valueOf(valeur));
    }

    private static String escape(String texte) {
        return texte
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static final class Parser {
        private final String texte;
        private int index;

        private Parser(String texte) {
            this.texte = texte;
            this.index = 0;
        }

        private boolean isEnd() {
            return index >= texte.length();
        }

        private void skipWhitespace() {
            while (!isEnd() && Character.isWhitespace(texte.charAt(index))) index++;
        }

        private Object parseValue() {
            skipWhitespace();
            if (isEnd()) throw new IllegalArgumentException("JSON vide.");
            char c = texte.charAt(index);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseTrue();
                case 'f' -> parseFalse();
                case 'n' -> parseNull();
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            index++; // {
            skipWhitespace();
            if (!isEnd() && texte.charAt(index) == '}') {
                index++;
                return map;
            }
            while (true) {
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return map;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            index++; // [
            skipWhitespace();
            if (!isEnd() && texte.charAt(index) == ']') {
                index++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return list;
                }
                expect(',');
            }
        }

        private String parseString() {
            skipWhitespace();
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (!isEnd()) {
                char c = texte.charAt(index++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (isEnd()) throw new IllegalArgumentException("JSON invalide.");
                    char escaped = texte.charAt(index++);
                    switch (escaped) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (index + 4 > texte.length()) throw new IllegalArgumentException("Unicode JSON invalide.");
                            String hex = texte.substring(index, index + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                        }
                        default -> throw new IllegalArgumentException("Échappement JSON invalide.");
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("Chaîne JSON non terminée.");
        }

        private Boolean parseTrue() {
            expectWord("true");
            return Boolean.TRUE;
        }

        private Boolean parseFalse() {
            expectWord("false");
            return Boolean.FALSE;
        }

        private Object parseNull() {
            expectWord("null");
            return null;
        }

        private Number parseNumber() {
            skipWhitespace();
            int debut = index;
            if (peek('-')) index++;
            while (!isEnd() && Character.isDigit(texte.charAt(index))) index++;
            boolean flottant = false;
            if (!isEnd() && texte.charAt(index) == '.') {
                flottant = true;
                index++;
                while (!isEnd() && Character.isDigit(texte.charAt(index))) index++;
            }
            if (!isEnd() && (texte.charAt(index) == 'e' || texte.charAt(index) == 'E')) {
                flottant = true;
                index++;
                if (!isEnd() && (texte.charAt(index) == '+' || texte.charAt(index) == '-')) index++;
                while (!isEnd() && Character.isDigit(texte.charAt(index))) index++;
            }
            String nombre = texte.substring(debut, index);
            if (nombre.isBlank() || nombre.equals("-")) {
                throw new IllegalArgumentException("Nombre JSON invalide.");
            }
            return flottant ? Double.parseDouble(nombre) : Long.parseLong(nombre);
        }

        private boolean peek(char attendu) {
            skipWhitespace();
            return !isEnd() && texte.charAt(index) == attendu;
        }

        private void expect(char attendu) {
            skipWhitespace();
            if (isEnd() || texte.charAt(index) != attendu) {
                throw new IllegalArgumentException("JSON invalide : caractère attendu " + attendu);
            }
            index++;
        }

        private void expectWord(String mot) {
            skipWhitespace();
            if (!texte.startsWith(mot, index)) throw new IllegalArgumentException("JSON invalide.");
            index += mot.length();
        }
    }
}
