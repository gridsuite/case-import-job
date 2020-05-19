/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.cases.importer.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

/**
 * @author Nicolas Noir <nicolas.noir at rte-france.com>
 */
public final class SftpCaseAcquisitionJob {

    private static final String CONFIG_FILE_NAME =  "case-import-job-configuration.yaml";

    private static final Logger LOGGER = LoggerFactory.getLogger(SftpCaseAcquisitionJob.class);

    private SftpCaseAcquisitionJob() {
    }

    public static void main(String... args)
            throws IOException, URISyntaxException {

        Path configFilePath = Path.of("/config", CONFIG_FILE_NAME);
        if (!Files.exists(configFilePath)) {
            URL url = Thread.currentThread().getContextClassLoader().getResource(CONFIG_FILE_NAME);
            configFilePath = Paths.get(url.toURI());
        }

        CaseImportJobConfiguration jobConfiguration = CaseImportJobConfiguration.parseFile(configFilePath);

        final CaseImportServiceRequester caseImportServiceRequester = new CaseImportServiceRequester(jobConfiguration.caseServerConfig.url);

        try (SftpConnection sftpConnection = new SftpConnection();
             CaseImportLogger caseImportLogger = new CaseImportLogger()) {

            sftpConnection.open(jobConfiguration.sftpServerConfig.hostname, jobConfiguration.sftpServerConfig.username, jobConfiguration.sftpServerConfig.password);
            caseImportLogger.connectDb(jobConfiguration.cassandraConfig.contactPoints, Integer.parseInt(jobConfiguration.cassandraConfig.port));

            String casesDirectory = jobConfiguration.sftpServerConfig.casesDirectory;
            List<Path> filesToAcquire = sftpConnection.listFiles(casesDirectory);
            for (Path file : filesToAcquire) {
                TransferableFile acquiredFile = sftpConnection.getFile(file.toString());
                if (!caseImportLogger.isImportedFile(acquiredFile.getName(), jobConfiguration.sftpServerConfig.label)) {
                    LOGGER.info("Import of : \"" + file.toString() + "\"");
                    boolean importOk = caseImportServiceRequester.importCase(acquiredFile);
                    if (importOk) {
                        caseImportLogger.logFileAcquired(acquiredFile.getName(), jobConfiguration.sftpServerConfig.label, new Date());
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
