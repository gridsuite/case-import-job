package org.gridsuite.cases.importer.job;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ExtendWith(MockServerExtension.class)
class CaseImportServiceRequesterTest {
    @Test
    void testCaseImportRequester(final MockServerClient mockServerClient) throws Exception {
        final CaseImportServiceRequester caseImportServiceRequester = new CaseImportServiceRequester("http://localhost:" + mockServerClient.getPort() + "/");
        mockServerClient.when(request().withMethod("POST").withPath("/v1/cases")).respond(response().withStatusCode(200));
        assertTrue(caseImportServiceRequester.importCase(new TransferableFile("case.iidm", "Case file content".getBytes(UTF_8))));
        mockServerClient.clear(request());
        mockServerClient.when(request().withMethod("POST").withPath("/v1/cases")).respond(response().withStatusCode(500));
        assertFalse(caseImportServiceRequester.importCase(new TransferableFile("case.iidm", "Case file content".getBytes(UTF_8))));
    }
}
