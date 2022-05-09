/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.cases.importer.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * @author Nicolas Noir <nicolas.noir at rte-france.com>
 */
public class CaseImportLogger implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseImportLogger.class);
    private static final String SELECT_QUERY = "SELECT (filename, origin, import_date) FROM files where filename = ? and origin = ?";
    private static final String INSERT_QUERY = "INSERT INTO files (filename, origin, import_date) VALUES(?, ?, ?)";

    private Connection connection;

    public CaseImportLogger(DataSource dataSource) {
        try {
            this.connection = dataSource.getConnection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isImportedFile(String filename, String origin) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_QUERY)) {
            preparedStatement.setString(1, filename);
            preparedStatement.setString(2, origin);
            ResultSet resultSet = preparedStatement.executeQuery();

            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void logFileAcquired(String fileName, String origin, Date date) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_QUERY)) {
            preparedStatement.setString(1, fileName);
            preparedStatement.setString(2, origin);
            preparedStatement.setDate(3, new java.sql.Date(date.getTime()));
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            LOGGER.error("Error closing connection", e);
        }
    }
}
