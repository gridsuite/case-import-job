/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.cases.importer.job;

import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import java.util.Date;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.*;
/**
 * @author Nicolas Noir <nicolas.noir at rte-france.com>
 */
public class CaseImportLogger implements AutoCloseable {

    private final CassandraConnector connector = new CassandraConnector();

    private static final String KEYSPACE_IMPORT_HISTORY = "import_history";

    private static final String FILES_TABLE = "files";

    private static final String FILENAME_COLUMN = "filename";
    private static final String ORIGIN_COLUMN = "origin";
    private static final String IMPORT_DATE_COLUMN = "import_date";

    private PreparedStatement psInsertImportedFile;

    public void connectDb(String hostname, int port) {
        connector.connect(hostname, port);

        psInsertImportedFile = connector.getSession().prepare(insertInto(KEYSPACE_IMPORT_HISTORY, FILES_TABLE)
                .value(FILENAME_COLUMN, bindMarker())
                .value(ORIGIN_COLUMN, bindMarker())
                .value(IMPORT_DATE_COLUMN, bindMarker())
                .build());

    }

    public boolean isImportedFile(String filename, String origin) {
        ResultSet resultSet = connector.getSession().execute(selectFrom(KEYSPACE_IMPORT_HISTORY, FILES_TABLE)
                .columns(
                        FILENAME_COLUMN,
                        ORIGIN_COLUMN,
                        IMPORT_DATE_COLUMN)
                .whereColumn(FILENAME_COLUMN).isEqualTo(literal(filename))
                .whereColumn(ORIGIN_COLUMN).isEqualTo(literal(origin))
                .build());
        Row one = resultSet.one();
        return one != null;
    }

    public void logFileAcquired(String fileName, String origin, Date date) {
        connector.getSession().execute(psInsertImportedFile.bind(fileName, origin, date.toInstant()));
    }

    public void close() {
        connector.close();
    }

}
