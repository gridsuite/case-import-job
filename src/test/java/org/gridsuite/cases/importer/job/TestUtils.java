package org.gridsuite.cases.importer.job;

import com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.ExceptionThrowingConsumer;
import org.springframework.lang.NonNull;

import static com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer;

public final class TestUtils {
    public static final String SFTP_LABEL = "my_sftp_server";

    private TestUtils() {
        throw new IllegalCallerException("Utility class");
    }

    public static void withSftp(@NonNull final ExceptionThrowingConsumer testCode) throws Exception {
        withSftpServer(server -> {
            server.addUser("dummy", "dummy")/*.setPort(2222)*/;
            server.createDirectory("cases");
            testCode.accept(server);
            server.deleteAllFilesAndDirectories();
        });
    }
}
