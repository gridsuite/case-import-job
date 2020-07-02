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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

        try (SftpConnection sftpConnection = new SftpConnection();
             CaseImportLogger caseImportLogger = new CaseImportLogger()) {

            sftpConnection.open(moduleConfigSftpServer.getStringProperty("hostname"),
                                moduleConfigSftpServer.getIntProperty("port", 22),
                                moduleConfigSftpServer.getStringProperty("username"),
                                moduleConfigSftpServer.getStringProperty("password"));

            caseImportLogger.connectDb(moduleConfigCassandra.getStringProperty("contact-points"), moduleConfigCassandra.getIntProperty("port"));

            String casesDirectory = moduleConfigSftpServer.getStringProperty("cases-directory");
            String sftpServerLabel = moduleConfigSftpServer.getStringProperty("label");
            List<Path> filesToAcquire = sftpConnection.listFiles(casesDirectory);
            LOGGER.info("{} files found on SFTP server", filesToAcquire.size());
            List<Path> filesImported = new ArrayList<>();
            List<Path> filesAlreadyImported = new ArrayList<>();
            List<Path> filesImportFailed = new ArrayList<>();
            for (Path file : filesToAcquire) {
                TransferableFile acquiredFile = sftpConnection.getFile(file.toString());
                if (!caseImportLogger.isImportedFile(acquiredFile.getName(), sftpServerLabel)) {
                    LOGGER.info("Importing file '{}'...", file);
                    boolean importOk = caseImportServiceRequester.importCase(acquiredFile);
                    if (importOk) {
                        caseImportLogger.logFileAcquired(acquiredFile.getName(), sftpServerLabel, new Date());
                        filesImported.add(file);
                    } else {
                        filesImportFailed.add(file);
                    }
                } else {
                    filesAlreadyImported.add(file);
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
