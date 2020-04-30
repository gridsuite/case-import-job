/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.cases.importer.job;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

/**
 * @author Nicolas Noir <nicolas.noir at rte-france.com>
 */
public class SftpAcquisitionJob {

    final static Path acquisitionPath = Path.of("./cases");

    public static void main(String... args)
            throws IOException, InterruptedException {

        final SftpConnection sftpConnection = SftpConnection.getInstance();
        final HttpRequester httpRequester = HttpRequester.getInstance();
        final AcquisitionLogger acquisitionLogger = AcquisitionLogger.getInstance();


        try {
            sftpConnection.open();
            acquisitionLogger.init();

            List<Path> filesToAcquire = sftpConnection.listFiles(acquisitionPath);
            for (Path file : filesToAcquire) {
                TransferableFile acquiredFile = sftpConnection.getFile(file.toString());
                if (!acquisitionLogger.isAcquiredFile(acquiredFile.getName(), "Origin")) {
                    System.out.println("Import of : \"" + file.toString() + "\"");
                    httpRequester.importCase(acquiredFile);
                    acquisitionLogger.logFileAcquired(acquiredFile.getName(), "Origin", new Date());
                } else {
                    System.out.println("File already imported : \"" + file.toString() + "\"");
                }
            }
        } finally {
            sftpConnection.close();
            acquisitionLogger.close();
        }
    }
}
