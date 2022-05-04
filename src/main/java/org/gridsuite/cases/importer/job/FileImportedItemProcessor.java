package org.gridsuite.cases.importer.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileImportedItemProcessor implements ItemProcessor<FileUrl, FileImported> {

    @Autowired
    private DataSource dataSource;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileImportedItemProcessor.class);
    private AcquisitionServer acquisitionServer;
    private CaseImportServiceRequester caseImportServiceRequester;
    private List<String> filesImported = new ArrayList<>();
    private List<String> filesAlreadyImported = new ArrayList<>();
    private List<String> filesImportFailed = new ArrayList<>();
    private String origin = "my_sftp_server";

    public FileImportedItemProcessor(AcquisitionServer acquisitionServer,
                                     CaseImportServiceRequester caseImportServiceRequester) {
        this.acquisitionServer = acquisitionServer;
        this.caseImportServiceRequester = caseImportServiceRequester;
    }

    @Override
    public FileImported process(FileUrl fileUrl) throws Exception {
        try (CaseImportLogger caseImportLogger = new CaseImportLogger(dataSource)) {
            if (!caseImportLogger.isImportedFile(fileUrl.getFilename(), origin)) {
                acquisitionServer.close();
                acquisitionServer.open();
                TransferableFile acquiredFile = acquisitionServer.getFile(fileUrl.getFilename(), fileUrl.getUrl());
                LOGGER.info("Importing file '{}'...", fileUrl.getFilename());
                boolean importOk = caseImportServiceRequester.importCase(acquiredFile);
                if (importOk) {
                    filesImported.add(fileUrl.getFilename());
                    return new FileImported(fileUrl.getFilename(), origin, new Date());
                } else {
                    filesImportFailed.add(fileUrl.getFilename());
                }
            } else {
                filesAlreadyImported.add(fileUrl.getFilename());
            }

            acquisitionServer.close();
        }

        return null;
    }

    public List<String> getFilesImported() {
        return filesImported;
    }

    public List<String> getFilesAlreadyImported() {
        return filesAlreadyImported;
    }

    public List<String> getFilesImportFailed() {
        return filesImportFailed;
    }
}
