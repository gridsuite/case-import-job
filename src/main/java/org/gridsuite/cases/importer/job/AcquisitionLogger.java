/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.cases.importer.job;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

import java.util.Date;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;

/**
 * @author Nicolas Noir <nicolas.noir at rte-france.com>
 */
public class AcquisitionLogger {

    private final CassandraConnector connector = new CassandraConnector();

    private static final String KEYSPACE_ACQUISITION_LOGS = "acquisition_logs";

    public static final AcquisitionLogger acquitionLogger = new AcquisitionLogger();

    public static AcquisitionLogger getInstance() {
        return acquitionLogger;
    }

    private PreparedStatement psInsertAcquiredFile;

    public void init(String hostname, int port) {
        connector.connect(hostname, port);

        psInsertAcquiredFile = connector.getSession().prepare(insertInto(KEYSPACE_ACQUISITION_LOGS, "files")
                .value("filename", bindMarker())
                .value("origin", bindMarker())
                .value("acq_date", bindMarker()));

    }

    public boolean isAcquiredFile(String filename, String origin) {
        ResultSet resultSet = connector.getSession().execute(select("filename",
                "origin",
                "acq_date")
                .from(KEYSPACE_ACQUISITION_LOGS, "files")
                .where(eq("filename", filename)).and(eq("origin", origin)));
        Row one = resultSet.one();
        if (one != null) {
            return true;
        }
        return false;
    }

    public void logFileAcquired(String fileName, String origin, Date date) {
        connector.getSession().execute(psInsertAcquiredFile.bind(fileName, origin, date));
    }


    public void close() {
        if (connector != null) {
            connector.close();
        }
    }

}
