package com.tukue.classroombooking.infrastructure.database;

import java.sql.Connection;
import java.sql.SQLException;

/** Abstracts creation of short-lived JDBC connections for persistence adapters. */
public interface ConnectionFactory {
    Connection openConnection() throws SQLException;
}
