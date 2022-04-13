/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.cases.importer.job;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;

/**
 * @author Nicolas Noir <nicolas.noir at rte-france.com>
 */
public class CaseImportLogger implements AutoCloseable {

    private static final String SELECT_QUERY = "SELECT (filename, origin, import_date) FROM files where filename = ? and origin = ?";
    private static final String INSERT_QUERY = "INSERT INTO files (filename, origin, import_date) VALUES(?, ?, ?)";

    public static final String DB_CHANGELOG_MASTER = "db/changelog/db.changelog-master.yaml";

    private JdbcConnector connector;

    public void connectDb(String url, String username, String password) {
        connector = new JdbcConnector(url, username, password);
        try {
            // liquibase creates the connection and closes it
            // (normal because it could use a separate user, or set special flags on the connection)
            updateLiquibase(connector);
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }

        // Create another connection for regular operations
        connector.connect();
    }

    // TODO use separate user/password for liquibase
    private void updateLiquibase(JdbcConnector connector) throws DatabaseException {
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connector.connect()));
        Properties properties = new Properties();
        try (Liquibase liquibase = new Liquibase(DB_CHANGELOG_MASTER, new ClassLoaderResourceAccessor(), database);) {
            properties.forEach((key, value) -> liquibase.setChangeLogParameter(Objects.toString(key), value));
            liquibase.update(new Contexts(), new LabelExpression());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isImportedFile(String filename, String origin) {
        try (PreparedStatement preparedStatement = connector.getConnection().prepareStatement(SELECT_QUERY)) {
            preparedStatement.setString(1, filename);
            preparedStatement.setString(2, origin);
            ResultSet resultSet = preparedStatement.executeQuery();

            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void logFileAcquired(String fileName, String origin, Date date) {
        try (PreparedStatement preparedStatement = connector.getConnection().prepareStatement(INSERT_QUERY)) {
            preparedStatement.setString(1, fileName);
            preparedStatement.setString(2, origin);
            preparedStatement.setDate(3, new java.sql.Date(date.getTime()));
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        connector.close();
    }
}
