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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Nicolas Noir <nicolas.noir at rte-france.com>
 */

@SuppressWarnings({"checkstyle:HideUtilityClassConstructor", "checkstyle:FinalClass"})
@SpringBootApplication
public class CaseAcquisitionJob implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseAcquisitionJob.class);

    @Autowired
    private DataSource dataSource;

    public static void main(String... args) {
        SpringApplication.run(CaseAcquisitionJob.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        PlatformConfig platformConfig = PlatformConfig.defaultConfig();

        ModuleConfig moduleConfigAcquisitionServer = platformConfig.getModuleConfig("acquisition-server");
        ModuleConfig moduleConfigCaseServer = platformConfig.getModuleConfig("case-server");

        final CaseImportServiceRequester caseImportServiceRequester = new CaseImportServiceRequester(moduleConfigCaseServer.getStringProperty("url"));

        try (AcquisitionServer acquisitionServer = new AcquisitionServer(moduleConfigAcquisitionServer.getStringProperty("url"),
                                                                         moduleConfigAcquisitionServer.getStringProperty("username"),
                                                                         moduleConfigAcquisitionServer.getStringProperty("password"));
             CaseImportLogger caseImportLogger = new CaseImportLogger(dataSource)) {
            acquisitionServer.open();

            String casesDirectory = moduleConfigAcquisitionServer.getStringProperty("cases-directory");
            String serverLabel = moduleConfigAcquisitionServer.getStringProperty("label");
            Map<String, String> filesToAcquire = acquisitionServer.listFiles(casesDirectory);
            LOGGER.info("{} files found on server", filesToAcquire.size());

            List<String> filesImported = new ArrayList<>();
            List<String> filesAlreadyImported = new ArrayList<>();
            List<String> filesImportFailed = new ArrayList<>();
            for (Map.Entry<String, String> fileInfo : filesToAcquire.entrySet()) {
                if (!caseImportLogger.isImportedFile(fileInfo.getKey(), serverLabel)) {
                    TransferableFile acquiredFile = acquisitionServer.getFile(fileInfo.getKey(), fileInfo.getValue());
                    LOGGER.info("Importing file '{}'...", fileInfo.getKey());
                    boolean importOk = caseImportServiceRequester.importCase(acquiredFile);
                    if (importOk) {
                        caseImportLogger.logFileAcquired(acquiredFile.getName(), serverLabel, new Date());
                        filesImported.add(fileInfo.getKey());
                    } else {
                        filesImportFailed.add(fileInfo.getKey());
                    }
                } else {
                    filesAlreadyImported.add(fileInfo.getKey());
                }
            }
            LOGGER.info("===== JOB EXECUTION SUMMARY =====");
            LOGGER.info("{} files already imported", filesAlreadyImported.size());
            LOGGER.info("{} files successfully imported", filesImported.size());
            filesImported.forEach(f -> LOGGER.info("File '{}' successfully imported", f));
            LOGGER.info("{} files import failed", filesImportFailed.size());
            filesImportFailed.forEach(f -> LOGGER.info("File '{}' import failed !!", f));
            LOGGER.info("=================================");
        }
    }
}
