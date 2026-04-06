package api;

import java.util.*;

public final class Json {

    private Json() {}


    public static Object parse(String texte) {
        if (texte == null) return null;
        Parser parser = new Parser(texte);
        Object valeur = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isEnd()) {
            parser.error("Caractères en trop après le JSON");
        }
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

        if (valeur instanceof Character c) {
            sb.append('"').append(escape(c.toString())).append('"');
            return;
        }

        if (valeur instanceof Map<?, ?> map) {
            sb.append('{');
            boolean premier = true;

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    throw new IllegalArgumentException("Clé JSON doit être une String");
                }

                if (!premier) sb.append(',');
                premier = false;

                appendValue(sb, entry.getKey());
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

        // fallback
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
        }

        private boolean isEnd() {
            return index >= texte.length();
        }

        private void skipWhitespace() {
            while (!isEnd() && Character.isWhitespace(texte.charAt(index))) {
                index++;
            }
        }

        private void error(String message) {
            throw new IllegalArgumentException(message + " à la position " + index);
        }


        private Object parseValue() {
            skipWhitespace();

            if (isEnd()) error("JSON vide");

            char c = texte.charAt(index);

            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseTrue();
                case 'f' -> parseFalse();
                case 'n' -> parseNull();
                default -> {
                    if (c == '-' || Character.isDigit(c)) {
                        yield parseNumber();
                    }
                    error("Caractère inattendu: " + c);
                    yield null;
                }
            };
        }



        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            index++; // {

            skipWhitespace();

            if (peek('}')) {
                index++;
                return map;
            }

            while (true) {
                if (!peek('"')) error("Clé JSON doit être une chaîne");

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

            if (peek(']')) {
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
                    if (isEnd()) error("Échappement invalide");

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
                            if (index + 4 > texte.length()) {
                                error("Unicode invalide");
                            }
                            String hex = texte.substring(index, index + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                        }
                        default -> error("Échappement invalide");
                    }
                } else {
                    sb.append(c);
                }
            }

            error("Chaîne non terminée");
            return null;
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

            int start = index;

            if (peek('-')) index++;

            while (!isEnd() && Character.isDigit(texte.charAt(index))) index++;

            boolean isFloat = false;

            if (!isEnd() && texte.charAt(index) == '.') {
                isFloat = true;
                index++;
                while (!isEnd() && Character.isDigit(texte.charAt(index))) index++;
            }

            if (!isEnd() && (texte.charAt(index) == 'e' || texte.charAt(index) == 'E')) {
                isFloat = true;
                index++;
                if (!isEnd() && (texte.charAt(index) == '+' || texte.charAt(index) == '-')) {
                    index++;
                }
                while (!isEnd() && Character.isDigit(texte.charAt(index))) index++;
            }

            String number = texte.substring(start, index);

            if (number.isBlank() || number.equals("-")) {
                error("Nombre invalide");
            }

            try {
                return isFloat ? Double.parseDouble(number) : Long.parseLong(number);
            } catch (NumberFormatException e) {
                return Double.parseDouble(number);
            }
        }



        private boolean peek(char attendu) {
            skipWhitespace();
            return !isEnd() && texte.charAt(index) == attendu;
        }

        private void expect(char attendu) {
            skipWhitespace();
            if (isEnd() || texte.charAt(index) != attendu) {
                error("Caractère attendu: " + attendu);
            }
            index++;
        }

        private void expectWord(String mot) {
            skipWhitespace();
            if (!texte.startsWith(mot, index)) {
                error("Mot attendu: " + mot);
            }
            index += mot.length();
        }
    }
}