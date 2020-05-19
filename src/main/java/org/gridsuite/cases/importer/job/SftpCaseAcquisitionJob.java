/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.cases.importer.job;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nicolas Noir <nicolas.noir at rte-france.com>
 */
public class SftpCaseAcquisitionJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(SftpCaseAcquisitionJob.class);

    public static void main(String... args)
            throws IOException, InterruptedException {

        // Loading properties
        Properties cassandraProperties = new Properties();
        try {
            cassandraProperties.load(new FileInputStream("/conf/cassandra.properties"));
        } catch (FileNotFoundException e){
            cassandraProperties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("cassandra.properties"));
        }

        Properties sftpProperties = new Properties();
        try {
            sftpProperties.load(new FileInputStream("/conf/sftp-server.properties"));
        } catch (FileNotFoundException e){
            sftpProperties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("sftp-server.properties"));
        }

        Properties sftpCredentialsProperties = new Properties();
        try {
            sftpCredentialsProperties.load(new FileInputStream("/.conf/sftp-credentials.properties"));
        } catch (FileNotFoundException e){
            sftpCredentialsProperties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("sftp-credentials.properties"));
        }

        Properties serviceProperties = new Properties();
        try {
            serviceProperties.load(new FileInputStream("/conf/case-import-service.properties"));
        } catch (FileNotFoundException e){
            serviceProperties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("case-import-service.properties"));
        }

        final CaseImportServiceRequester caseImportServiceRequester = new CaseImportServiceRequester(serviceProperties.getProperty("service.url"));

        try (SftpConnection sftpConnection = new SftpConnection();
             CaseImportLogger caseImportLogger = new CaseImportLogger()) {

            sftpConnection.open(sftpProperties.getProperty("hostname"), sftpCredentialsProperties.getProperty("user.name"), sftpCredentialsProperties.getProperty("password"));
            caseImportLogger.connectDb(cassandraProperties.getProperty("cassandra.contact-points"), Integer.parseInt(cassandraProperties.getProperty("cassandra.port")));

            String casesDirectory = sftpProperties.get("cases.directory").toString();
            List<Path> filesToAcquire = sftpConnection.listFiles(casesDirectory);
            for (Path file : filesToAcquire) {
                TransferableFile acquiredFile = sftpConnection.getFile(file.toString());
                if (!caseImportLogger.isImportedFile(acquiredFile.getName(), sftpProperties.getProperty("label"))) {
                    LOGGER.info("Import of : \"" + file.toString() + "\"");
                    boolean importOk = caseImportServiceRequester.importCase(acquiredFile);
                    if (importOk) {
                        caseImportLogger.logFileAcquired(acquiredFile.getName(), sftpProperties.getProperty("label"), new Date());
                    }
                } else {
                    LOGGER.info("File already imported : \"" + file.toString() + "\"");
                }
            }
        } catch (Exception exc) {
          LOGGER.error("Job execution error: " + exc.getMessage());
        }
    }
}
