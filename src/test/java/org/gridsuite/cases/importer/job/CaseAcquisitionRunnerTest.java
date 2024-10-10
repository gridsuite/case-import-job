package org.gridsuite.cases.importer.job;

import com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import javax.sql.DataSource;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ExtendWith(MockServerExtension.class)
@SpringBootTest(classes = { CaseAcquisitionJob.class })
class CaseAcquisitionRunnerTest {
    @Autowired private DataSource dataSource;
    @MockBean private CaseAcquisitionJob clRunner; //prevent runner to run before tests

    @Test
    void mainTest(final MockServerClient mockServerClient) throws Exception {
        final CaseAcquisitionJob runner = new CaseAcquisitionJob(dataSource);
        TestUtils.withSftp(server -> {
            server.putFile("cases/case1.iidm", "fake file content 1", UTF_8);
            server.putFile("cases/case2.iidm", "fake file content 2", UTF_8);
            try (final CaseImportLogger caseImportLogger = new CaseImportLogger(dataSource)) {

                // 2 files on SFTP server, 2 cases will be imported
                mockServerClient.when(request().withMethod("POST").withPath("/v1/cases")).respond(response().withStatusCode(200));
                runJob(runner, server, mockServerClient);
                mockServerClient.verify(request().withMethod("POST").withPath("/v1/cases"), VerificationTimes.exactly(2));
                assertTrue(caseImportLogger.isImportedFile("case1.iidm", TestUtils.SFTP_LABEL));
                assertTrue(caseImportLogger.isImportedFile("case2.iidm", TestUtils.SFTP_LABEL));

                // No new files on SFTP server, no import requested
                mockServerClient.clear(request());
                mockServerClient.when(request().withMethod("POST").withPath("/v1/cases")).respond(response().withStatusCode(200));
                runJob(runner, server, mockServerClient);
                mockServerClient.verify(request().withMethod("POST").withPath("/v1/cases"), VerificationTimes.exactly(0));

                // One new file on SFTP server, one case import requested
                mockServerClient.clear(request());
                mockServerClient.when(request().withMethod("POST").withPath("/v1/cases")).respond(response().withStatusCode(200));
                server.putFile("cases/case3.iidm", "fake file content 3", UTF_8);
                runJob(runner, server, mockServerClient);
                mockServerClient.verify(request().withMethod("POST").withPath("/v1/cases"), VerificationTimes.exactly(1));
                assertTrue(caseImportLogger.isImportedFile("case3.iidm", TestUtils.SFTP_LABEL));

                // One new file on server, but error when requesting import: case is not logged as imported
                mockServerClient.clear(request());
                mockServerClient.when(request().withMethod("POST").withPath("/v1/cases")).respond(response().withStatusCode(500));
                server.putFile("cases/case4.iidm", "fake file content 4", UTF_8);
                runJob(runner, server, mockServerClient);
                mockServerClient.verify(request().withMethod("POST").withPath("/v1/cases"), VerificationTimes.exactly(1));
                assertFalse(caseImportLogger.isImportedFile("case4.iidm", TestUtils.SFTP_LABEL));
            }
        });
    }

    private static void runJob(final CaseAcquisitionJob runner, final FakeSftpServer sftpServer, final MockServerClient mockServerClient) throws Exception {
        runner.run(
                "http://localhost:" + mockServerClient.getPort() + "/",
                "sftp://localhost:" + sftpServer.getPort(), //random free port
                "dummy", //server.addUser(...)
                "dummy",
                "./cases", //server.createDirectory(...)
                TestUtils.SFTP_LABEL
        );
    }
}
