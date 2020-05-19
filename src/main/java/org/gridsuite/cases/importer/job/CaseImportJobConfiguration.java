/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.cases.importer.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Nicolas Noir <nicolas.noir at rte-france.com>
 */
public class CaseImportJobConfiguration {

    public class SftpServerConfig {

        @JsonProperty
        String hostname;

        @JsonProperty
        String username;

        @JsonProperty
        String password;

        @JsonProperty("cases-directory")
        String casesDirectory;

        @JsonProperty
        String label;
    }

    public class CassandraConfig {

        @JsonProperty("contact-points")
        String contactPoints;

        @JsonProperty
        String port;
    }

    public class CaseServerConfig {
        @JsonProperty
        String url;
    }

    @JsonProperty("sftp-server")
    public SftpServerConfig sftpServerConfig;

    @JsonProperty("cassandra")
    public CassandraConfig cassandraConfig;

    @JsonProperty("case-server")
    public CaseServerConfig caseServerConfig;

    public static CaseImportJobConfiguration parseFile(Path configFilePath) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        final String yamlSource = new String(Files.readAllBytes(configFilePath), StandardCharsets.UTF_8);
        return mapper.readValue(yamlSource, CaseImportJobConfiguration.class);
    }
}
