package com.classroombooking.infrastructure.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.stream.Collectors;

/** Creates the relational schema and applies idempotent starter data at application startup. */
public final class DatabaseInitializer {
    private final ConnectionFactory connectionFactory;

    public DatabaseInitializer(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    /** Applies schema and seed scripts as one transaction. */
    public void initialize() throws SQLException, IOException {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(false);
            try {
                executeScript(connection, "/db/schema.sql");
                executeScript(connection, "/db/seed.sql");
                connection.commit();
            } catch (SQLException | IOException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void executeScript(Connection connection, String resourcePath)
            throws IOException, SQLException {
        String script = readResource(resourcePath);
        try (Statement statement = connection.createStatement()) {
            for (String command : script.split(";")) {
                String sql = command.strip();
                if (!sql.isEmpty()) {
                    statement.execute(sql);
                }
            }
        }
    }

    private String readResource(String resourcePath) throws IOException {
        InputStream inputStream = DatabaseInitializer.class.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("Database script not found: " + resourcePath);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .filter(line -> !line.stripLeading().startsWith("--"))
                    .collect(Collectors.joining("\n"));
        }
    }
}
