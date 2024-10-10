package org.gridsuite.cases.importer.job;

import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AcquisitionServerTest {
    @Test
    void testSftpAcquisition() throws Exception {
        TestUtils.withSftp(server -> {
            server.putFile("cases/case1.iidm", "fake file content 1", UTF_8);
            server.putFile("cases/case2.iidm", "fake file content 2", UTF_8);
            final String acquisitionServerUrl = "sftp://localhost:" + server.getPort();
            try (final AcquisitionServer acquisitionServer = new AcquisitionServer(acquisitionServerUrl, "dummy", "dummy")) {
                acquisitionServer.open();
                Map<String, String> retrievedFiles = acquisitionServer.listFiles("./cases");
                assertEquals(2, retrievedFiles.size());

                TransferableFile file1 = acquisitionServer.getFile("case1.iidm", acquisitionServerUrl + "/cases/case1.iidm");
                assertEquals("case1.iidm", file1.getName());
                assertEquals("fake file content 1", new String(file1.getData(), UTF_8));

                TransferableFile file2 = acquisitionServer.getFile("case2.iidm", acquisitionServerUrl + "/cases/case2.iidm");
                assertEquals("case2.iidm", file2.getName());
                assertEquals("fake file content 2", new String(file2.getData(), UTF_8));
            }
        });
    }

    @Test
    void testFtpAcquisition() throws Exception {
        final FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/cases"));
        fileSystem.add(new FileEntry("/cases/case1.iidm", "fake file content 1"));
        fileSystem.add(new FileEntry("/cases/case2.iidm", "fake file content 2"));

        final FakeFtpServer fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.addUserAccount(new UserAccount("dummy_ftp", "dummy_ftp", "/"));
        fakeFtpServer.setFileSystem(fileSystem);
        fakeFtpServer.setServerControlPort(0);

        try {
            fakeFtpServer.start();
            final String acquisitionServerUrl = "ftp://localhost:" + fakeFtpServer.getServerControlPort();
            try (final AcquisitionServer acquisitionServer = new AcquisitionServer(acquisitionServerUrl, "dummy_ftp", "dummy_ftp")) {
                acquisitionServer.open();
                Map<String, String> retrievedFiles = acquisitionServer.listFiles("./cases");
                assertEquals(2, retrievedFiles.size());

                TransferableFile file1 = acquisitionServer.getFile("case1.iidm", acquisitionServerUrl + "/cases/case1.iidm");
                assertEquals("case1.iidm", file1.getName());
                assertEquals("fake file content 1", new String(file1.getData(), UTF_8));

                TransferableFile file2 = acquisitionServer.getFile("case2.iidm", acquisitionServerUrl + "/cases/case2.iidm");
                assertEquals("case2.iidm", file2.getName());
                assertEquals("fake file content 2", new String(file2.getData(), UTF_8));
            }
        } finally {
            fakeFtpServer.stop();
        }
    }
}
