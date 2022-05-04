package org.gridsuite.cases.importer.job;

public class FileUrl {

    private String filename;
    private String url;

    FileUrl(String filename, String url) {
        this.filename = filename;
        this.url = url;
    }

    public String getFilename() {
        return filename;
    }

    public String getUrl() {
        return url;
    }
}
