package com.classroombooking.infrastructure.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Splits SQL scripts at statement terminators outside strings, quoted identifiers, and comments. */
public final class SqlScriptParser {
    private SqlScriptParser() {
    }

    public static List<String> splitStatements(String script) {
        Objects.requireNonNull(script, "script");
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        State state = State.NORMAL;
        String dollarDelimiter = null;
        int blockCommentDepth = 0;
        boolean escapeString = false;

        for (int index = 0; index < script.length(); index++) {
            char character = script.charAt(index);
            char next = nextCharacter(script, index);

            switch (state) {
                case NORMAL -> {
                    if (character == '-' && next == '-') {
                        state = State.LINE_COMMENT;
                        index++;
                    } else if (character == '/' && next == '*') {
                        current.append(' ');
                        state = State.BLOCK_COMMENT;
                        blockCommentDepth = 1;
                        index++;
                    } else if (character == '\'') {
                        escapeString = hasEscapeStringPrefix(current);
                        current.append(character);
                        state = State.SINGLE_QUOTED;
                    } else if (character == '"') {
                        current.append(character);
                        state = State.DOUBLE_QUOTED;
                    } else if (character == '$') {
                        dollarDelimiter = dollarDelimiterAt(script, index);
                        if (dollarDelimiter == null) {
                            current.append(character);
                        } else {
                            current.append(dollarDelimiter);
                            index += dollarDelimiter.length() - 1;
                            state = State.DOLLAR_QUOTED;
                        }
                    } else if (character == ';') {
                        addStatement(statements, current);
                    } else {
                        current.append(character);
                    }
                }
                case SINGLE_QUOTED -> {
                    current.append(character);
                    if (escapeString && character == '\\' && index + 1 < script.length()) {
                        current.append(script.charAt(++index));
                    } else if (character == '\'') {
                        if (next == '\'') {
                            current.append(script.charAt(++index));
                        } else {
                            state = State.NORMAL;
                        }
                    }
                }
                case DOUBLE_QUOTED -> {
                    current.append(character);
                    if (character == '"') {
                        if (next == '"') {
                            current.append(script.charAt(++index));
                        } else {
                            state = State.NORMAL;
                        }
                    }
                }
                case LINE_COMMENT -> {
                    if (character == '\n') {
                        current.append('\n');
                        state = State.NORMAL;
                    }
                }
                case BLOCK_COMMENT -> {
                    if (character == '/' && next == '*') {
                        blockCommentDepth++;
                        index++;
                    } else if (character == '*' && next == '/') {
                        blockCommentDepth--;
                        index++;
                        if (blockCommentDepth == 0) {
                            state = State.NORMAL;
                        }
                    }
                }
                case DOLLAR_QUOTED -> {
                    if (script.startsWith(dollarDelimiter, index)) {
                        current.append(dollarDelimiter);
                        index += dollarDelimiter.length() - 1;
                        dollarDelimiter = null;
                        state = State.NORMAL;
                    } else {
                        current.append(character);
                    }
                }
            }
        }

        if (state == State.SINGLE_QUOTED || state == State.DOUBLE_QUOTED
                || state == State.BLOCK_COMMENT || state == State.DOLLAR_QUOTED) {
            throw new IllegalArgumentException("Unterminated SQL string, identifier, or comment.");
        }
        addStatement(statements, current);
        return List.copyOf(statements);
    }

    private static char nextCharacter(String script, int index) {
        return index + 1 < script.length() ? script.charAt(index + 1) : '\0';
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().strip();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
        current.setLength(0);
    }

    private static boolean hasEscapeStringPrefix(StringBuilder current) {
        int prefixIndex = current.length() - 1;
        if (prefixIndex < 0 || (current.charAt(prefixIndex) != 'E'
                && current.charAt(prefixIndex) != 'e')) {
            return false;
        }
        return prefixIndex == 0 || !Character.isLetterOrDigit(current.charAt(prefixIndex - 1))
                && current.charAt(prefixIndex - 1) != '_';
    }

    private static String dollarDelimiterAt(String script, int startIndex) {
        int endIndex = script.indexOf('$', startIndex + 1);
        if (endIndex < 0) {
            return null;
        }
        String tag = script.substring(startIndex + 1, endIndex);
        if (tag.isEmpty() || (Character.isLetter(tag.charAt(0)) || tag.charAt(0) == '_')
                && tag.chars().allMatch(character -> Character.isLetterOrDigit(character)
                || character == '_')) {
            return script.substring(startIndex, endIndex + 1);
        }
        return null;
    }

    private enum State {
        NORMAL,
        SINGLE_QUOTED,
        DOUBLE_QUOTED,
        LINE_COMMENT,
        BLOCK_COMMENT,
        DOLLAR_QUOTED
    }
}
