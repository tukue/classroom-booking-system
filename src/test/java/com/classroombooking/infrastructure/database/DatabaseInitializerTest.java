package com.classroombooking.infrastructure.database;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseInitializerTest {
    @Test
    void rollsBackWhenASchemaStatementFails() {
        AtomicBoolean rollbackCalled = new AtomicBoolean();
        Statement statement = proxy(Statement.class, (proxy, method, args) -> {
            if (method.getName().equals("execute")) {
                throw new SQLException("invalid schema statement");
            }
            return null;
        });
        Connection connection = proxy(Connection.class, (proxy, method, args) -> {
            if (method.getName().equals("createStatement")) {
                return statement;
            }
            if (method.getName().equals("rollback")) {
                rollbackCalled.set(true);
            }
            return null;
        });
        DatabaseInitializer initializer = new DatabaseInitializer(() -> connection);

        assertThrows(SQLException.class, initializer::initialize);
        assertTrue(rollbackCalled.get());
    }

    private <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
