package org.gridsuite.cases.importer.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
@SpringBootApplication()
@EnableScheduling
public class CaseImportJobApp {

    public static void main(String[] args) {

        System.exit(SpringApplication.exit(SpringApplication.run(CaseImportJobApp.class, args)));
    }
}
