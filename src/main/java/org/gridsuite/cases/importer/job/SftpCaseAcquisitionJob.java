/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.cases.importer.job;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Nicolas Noir <nicolas.noir at rte-france.com>
 */
public final class SftpCaseAcquisitionJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(SftpCaseAcquisitionJob.class);

    private SftpCaseAcquisitionJob() {
    }

    public static void main(String... args) {

        PlatformConfig platformConfig = PlatformConfig.defaultConfig();

        ModuleConfig moduleConfigSftpServer = platformConfig.getModuleConfig("sftp-server");
        ModuleConfig moduleConfigCassandra = platformConfig.getModuleConfig("cassandra");
        ModuleConfig moduleConfigCaseServer = platformConfig.getModuleConfig("case-server");

        final CaseImportServiceRequester caseImportServiceRequester = new CaseImportServiceRequester(moduleConfigCaseServer.getStringProperty("url"));

        try (AcquisitionServer sftpConnection = new AcquisitionServer();
             CaseImportLogger caseImportLogger = new CaseImportLogger()) {

            sftpConnection.configure(moduleConfigSftpServer.getStringProperty("url"),
                                moduleConfigSftpServer.getIntProperty("port", 22),
                                moduleConfigSftpServer.getStringProperty("username"),
                                moduleConfigSftpServer.getStringProperty("password"));

            caseImportLogger.connectDb(moduleConfigCassandra.getStringProperty("contact-points"), moduleConfigCassandra.getIntProperty("port"));

            String casesDirectory = moduleConfigSftpServer.getStringProperty("cases-directory");
            String sftpServerLabel = moduleConfigSftpServer.getStringProperty("label");
            Map<String, String> filesToAcquire = sftpConnection.listFiles(casesDirectory);
            LOGGER.info("{} files found on SFTP server", filesToAcquire.size());

            List<String> filesImported = new ArrayList<>();
            List<String> filesAlreadyImported = new ArrayList<>();
            List<String> filesImportFailed = new ArrayList<>();
            for (String fileName : filesToAcquire.keySet()) {
                LOGGER.info("Retrieving file '{}'...", fileName);
                if (!caseImportLogger.isImportedFile(fileName, sftpServerLabel)) {
                    TransferableFile acquiredFile = sftpConnection.getFile(fileName, filesToAcquire.get(fileName));
                    LOGGER.info("Importing file '{}'...", fileName);
                    boolean importOk = caseImportServiceRequester.importCase(acquiredFile);
                    if (importOk) {
                        caseImportLogger.logFileAcquired(acquiredFile.getName(), sftpServerLabel, new Date());
                        filesImported.add(fileName);
                    } else {
                        filesImportFailed.add(fileName);
                    }
                } else {
                    filesAlreadyImported.add(fileName);
                }
            }
            LOGGER.info("===== JOB EXECUTION SUMMARY =====");
            LOGGER.info("{} files already imported", filesAlreadyImported.size());
            LOGGER.info("{} files successfully imported", filesImported.size());
            filesImported.forEach(f -> LOGGER.info("File '{}' successfully imported", f));
            LOGGER.info("{} files import failed", filesImportFailed.size());
            filesImportFailed.forEach(f -> LOGGER.info("File '{}' import failed !!", f));
            LOGGER.info("=================================");

        } catch (Exception exc) {
            LOGGER.error("Job execution error: {}", exc.getMessage());
        }
    }
}
