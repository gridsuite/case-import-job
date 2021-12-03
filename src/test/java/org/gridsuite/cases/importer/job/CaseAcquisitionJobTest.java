/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.cases.importer.job;

import com.github.nosan.embedded.cassandra.api.cql.CqlDataSet;
import com.github.nosan.embedded.cassandra.junit4.test.CassandraRule;
import com.github.stefanbirkner.fakesftpserver.rule.FakeSftpServerRule;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.mockserver.junit.MockServerRule;
import org.mockserver.verify.VerificationTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author Nicolas Noir <nicolas.noir at rte-france.com>
 */
public class CaseAcquisitionJobTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseAcquisitionJobTest.class);

    @ClassRule
    public static final CassandraRule CASSANDRA_RULE = new CassandraRule().withCassandraFactory(EmbeddedCassandraFactoryConfig.embeddedCassandraFactory())
                                                                          .withCqlDataSet(CqlDataSet.ofClasspaths("create_keyspace.cql", "import_history.cql"));

    @ClassRule
    public static final FakeSftpServerRule SFTP_SERVER_RULE = new FakeSftpServerRule().addUser("dummy", "dummy").setPort(2222);

    @Rule
    public final MockServerRule mockServer = new MockServerRule(this, 45385);

    @After
    public void tearDown() throws IOException {
        CqlDataSet.ofClasspaths("truncate.cql").forEachStatement(CASSANDRA_RULE.getCassandraConnection()::execute);
        SFTP_SERVER_RULE.deleteAllFilesAndDirectories();
    }

    @Test
    public void historyLoggerTest() {
        try (CaseImportLogger caseImportLogger = new CaseImportLogger()) {
            caseImportLogger.connectDb("localhost", 9142, "datacenter1", "import_history");
            Date importDate = new Date();
            assertFalse(caseImportLogger.isImportedFile("testFile.iidm", "my_sftp_server"));
            caseImportLogger.logFileAcquired("testFile.iidm", "my_sftp_server", importDate);
            assertTrue(caseImportLogger.isImportedFile("testFile.iidm", "my_sftp_server"));
        }
    }

    @Test
    public void testSftpAcquisition() throws IOException {

        SFTP_SERVER_RULE.createDirectory("/cases");
        SFTP_SERVER_RULE.putFile("/cases/case1.iidm", "fake file content 1", UTF_8);
        SFTP_SERVER_RULE.putFile("/cases/case2.iidm", "fake file content 2", UTF_8);
        String acquisitionServerUrl = "sftp://localhost:" + SFTP_SERVER_RULE.getPort();

        try (AcquisitionServer acquisitionServer = new AcquisitionServer(acquisitionServerUrl, "dummy", "dummy")) {
            acquisitionServer.open();
            Map<String, String> retrievedFiles = acquisitionServer.listFiles("./cases");
            assertEquals(2, retrievedFiles.size());

            TransferableFile file1 = acquisitionServer.getFile("case1.iidm", acquisitionServerUrl + "/cases/case1.iidm");
            assertEquals("case1.iidm", file1.getName());
            assertEquals("fake file content 1", new String(file1.getData(), UTF_8));

            TransferableFile file2 = acquisitionServer.getFile("case2.iidm", acquisitionServerUrl + "/cases/case2.iidm");
            assertEquals("case2.iidm", file2.getName());
            assertEquals("fake file content 2", new String(file2.getData(), UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testFtpAcquisition() throws IOException {

        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/cases"));
        fileSystem.add(new FileEntry("/cases/case1.iidm", "fake file content 1"));
        fileSystem.add(new FileEntry("/cases/case2.iidm", "fake file content 2"));

        FakeFtpServer fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.addUserAccount(new UserAccount("dummy_ftp", "dummy_ftp", "/"));
        fakeFtpServer.setFileSystem(fileSystem);
        fakeFtpServer.setServerControlPort(0);

        fakeFtpServer.start();

        String acquisitionServerUrl = "ftp://localhost:" + fakeFtpServer.getServerControlPort();
        try (AcquisitionServer acquisitionServer = new AcquisitionServer(acquisitionServerUrl, "dummy_ftp", "dummy_ftp")) {
            acquisitionServer.open();
            Map<String, String> retrievedFiles = acquisitionServer.listFiles("./cases");
            assertEquals(2, retrievedFiles.size());

            TransferableFile file1 = acquisitionServer.getFile("case1.iidm", acquisitionServerUrl + "/cases/case1.iidm");
            assertEquals("case1.iidm", file1.getName());
            assertEquals("fake file content 1", new String(file1.getData(), UTF_8));

            TransferableFile file2 = acquisitionServer.getFile("case2.iidm", acquisitionServerUrl + "/cases/case2.iidm");
            assertEquals("case2.iidm", file2.getName());
            assertEquals("fake file content 2", new String(file2.getData(), UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fakeFtpServer.stop();
        }

    }

    @Test
    public void testCaseImportRequester() throws IOException, InterruptedException {
        CaseImportServiceRequester caseImportServiceRequester = new CaseImportServiceRequester("http://localhost:45385/");
        mockServer.getClient().when(request().withMethod("POST").withPath("/v1/cases/public"))
                .respond(response().withStatusCode(200));
        assertTrue(caseImportServiceRequester.importCase(new TransferableFile("case.iidm", new String("Case file content").getBytes(UTF_8))));
        mockServer.getClient().clear(request());
        mockServer.getClient().when(request().withMethod("POST").withPath("/v1/cases/public"))
                .respond(response().withStatusCode(500));
        assertFalse(caseImportServiceRequester.importCase(new TransferableFile("case.iidm", new String("Case file content").getBytes(UTF_8))));
    }

    @Test
    public void mainTest() throws InterruptedException, IOException {

        SFTP_SERVER_RULE.createDirectory("/cases");
        SFTP_SERVER_RULE.putFile("/cases/case1.iidm", "fake file content 1", UTF_8);
        SFTP_SERVER_RULE.putFile("/cases/case2.iidm", "fake file content 2", UTF_8);

        CaseImportLogger caseImportLogger = new CaseImportLogger();
        caseImportLogger.connectDb("localhost", 9142, "datacenter1", "import_history");

        String[] args = null;

        // 2 files on SFTP server, 2 cases will be imported
        mockServer.getClient().when(request().withMethod("POST").withPath("/v1/cases/public"))
            .respond(response().withStatusCode(200));
        CaseAcquisitionJob.main(args);
        mockServer.getClient().verify(request().withMethod("POST").withPath("/v1/cases/public"), VerificationTimes.exactly(2));
        assertTrue(caseImportLogger.isImportedFile("case1.iidm", "my_sftp_server"));
        assertTrue(caseImportLogger.isImportedFile("case2.iidm", "my_sftp_server"));

        // No new files on SFTP server, no import requested
        mockServer.getClient().clear(request());
        mockServer.getClient().when(request().withMethod("POST").withPath("/v1/cases/public"))
                .respond(response().withStatusCode(200));
        CaseAcquisitionJob.main(args);
        mockServer.getClient().verify(request().withMethod("POST").withPath("/v1/cases/public"), VerificationTimes.exactly(0));

        // One new file on SFTP server, one case import requested
        mockServer.getClient().clear(request());
        mockServer.getClient().when(request().withMethod("POST").withPath("/v1/cases/public"))
                .respond(response().withStatusCode(200));
        SFTP_SERVER_RULE.putFile("/cases/case3.iidm", "fake file content 3", UTF_8);
        CaseAcquisitionJob.main(args);
        mockServer.getClient().verify(request().withMethod("POST").withPath("/v1/cases/public"), VerificationTimes.exactly(1));
        assertTrue(caseImportLogger.isImportedFile("case3.iidm", "my_sftp_server"));

        // One new file on server, but error when requesting import: case is not logged as imported
        mockServer.getClient().clear(request());
        mockServer.getClient().when(request().withMethod("POST").withPath("/v1/cases/public"))
                .respond(response().withStatusCode(500));
        SFTP_SERVER_RULE.putFile("/cases/case4.iidm", "fake file content 4", UTF_8);
        CaseAcquisitionJob.main(args);
        mockServer.getClient().verify(request().withMethod("POST").withPath("/v1/cases/public"), VerificationTimes.exactly(1));
        assertFalse(caseImportLogger.isImportedFile("case4.iidm", "my_sftp_server"));
    }

}
