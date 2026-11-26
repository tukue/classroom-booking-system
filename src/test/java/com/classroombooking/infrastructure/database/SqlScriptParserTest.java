package com.classroombooking.infrastructure.database;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlScriptParserTest {
    @Test
    void doesNotSplitSemicolonsInsideRegularAndEscapeStringLiterals() {
        String script = "INSERT INTO notes VALUES ('first;part', 'it''s; safe', E'escaped\\';quote'); "
                + "SELECT 2;";

        assertEquals(List.of(
                "INSERT INTO notes VALUES ('first;part', 'it''s; safe', E'escaped\\';quote')",
                "SELECT 2"), SqlScriptParser.splitStatements(script));
    }

    @Test
    void doesNotSplitInsideQuotedIdentifiersOrComments() {
        String script = "-- comment;\nSELECT \"semi;colon\" /* outer; /* nested; */ done; */;"
                + " -- trailing;";

        assertEquals(List.of("SELECT \"semi;colon\""),
                SqlScriptParser.splitStatements(script));
    }

    @Test
    void keepsPostgresDollarQuotedBlocksTogether() {
        String script = "DO $constraints$ BEGIN PERFORM ';'; PERFORM 2; END; $constraints$; SELECT 3;";

        assertEquals(List.of(
                "DO $constraints$ BEGIN PERFORM ';'; PERFORM 2; END; $constraints$",
                "SELECT 3"), SqlScriptParser.splitStatements(script));
    }

    @Test
    void rejectsUnterminatedQuotedTextAndComments() {
        assertThrows(IllegalArgumentException.class,
                () -> SqlScriptParser.splitStatements("SELECT 'not closed"));
        assertThrows(IllegalArgumentException.class,
                () -> SqlScriptParser.splitStatements("SELECT 1 /* not closed"));
        assertThrows(IllegalArgumentException.class,
                () -> SqlScriptParser.splitStatements("DO $tag$ BEGIN SELECT 1;"));
    }
}
