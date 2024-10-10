/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.cases.importer.job;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.sql.DataSource;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Nicolas Noir <nicolas.noir at rte-france.com>
 */
@JdbcTest
class CaseImportLoggerTest {
    @Autowired private DataSource dataSource;
    @MockBean private CaseAcquisitionJob job; //we don't want to run the cli runner

    @Test
    void historyLoggerTest() {
        try (CaseImportLogger caseImportLogger = new CaseImportLogger(dataSource)) {
            Date importDate = new Date();
            assertFalse(caseImportLogger.isImportedFile("testFile.iidm", TestUtils.SFTP_LABEL));
            caseImportLogger.logFileAcquired("testFile.iidm", TestUtils.SFTP_LABEL, importDate);
            assertTrue(caseImportLogger.isImportedFile("testFile.iidm", TestUtils.SFTP_LABEL));
        }
    }

    @Test
    void testLogFileAcquiredError() {
        assertThrows(RuntimeException.class, () -> {
            try (CaseImportLogger caseImportLogger = new CaseImportLogger(dataSource)) {
                Date importDate = new Date();
                caseImportLogger.logFileAcquired("test.iidm", null, importDate);
                caseImportLogger.logFileAcquired(null, TestUtils.SFTP_LABEL, importDate);
            }
        });
    }

    @Test
    void testIsLogFileAcquiredException() {
        assertThrows(RuntimeException.class, () -> {
            try (final CaseImportLogger caseImportLogger = new CaseImportLogger(dataSource)) {
                caseImportLogger.logFileAcquired(null, null, null);
            }
        });
    }

    @SuppressWarnings("EmptyTryBlock")
    @Test
    void testDatasourceNullException() {
        assertThrows(RuntimeException.class, () -> {
            try (final CaseImportLogger caseImportLogger = new CaseImportLogger(null)) {
                /* case not accessible */
            }
        });
    }
}
