/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.cases.importer.job;

import com.github.nosan.embedded.cassandra.api.cql.CqlDataSet;
import com.github.nosan.embedded.cassandra.junit4.test.CassandraRule;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Nicolas Noir <nicolas.noir at rte-france.com>
 */
public class SftpCaseAcquisitionJobTest {

    @ClassRule
    public static final CassandraRule CASSANDRA_RULE = new CassandraRule().withCassandraFactory(EmbeddedCassandraFactoryConfig.embeddedCassandraFactory())
                                                                          .withCqlDataSet(CqlDataSet.ofClasspaths("create_keyspace.cql", "import_history.cql"));

    @Test
    public void historyLoggerTest() {
        CaseImportLogger caseImportLogger = new CaseImportLogger();
        caseImportLogger.connectDb("localhost", 9142);
        Date importDate = new Date();
        assertFalse(caseImportLogger.isImportedFile("testFile.iidm", "testOrigin"));
        caseImportLogger.logFileAcquired("testFile.iidm", "testOrigin", importDate);
        assertTrue(caseImportLogger.isImportedFile("testFile.iidm", "testOrigin"));
    }

}
