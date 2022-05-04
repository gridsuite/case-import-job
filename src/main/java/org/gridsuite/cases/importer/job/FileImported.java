package org.gridsuite.cases.importer.job;

import java.io.Serializable;
import java.util.Date;

public class FileImported implements Serializable {
    private String filename;
    private String origin;
    private Date importedDate;

    public FileImported(String filename, String origin, Date importedDate) {
        this.filename = filename;
        this.origin = origin;
        this.importedDate = importedDate;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public void setImportedDate(Date importedDate) {
        this.importedDate = importedDate;
    }

    public String getFilename() {
        return filename;
    }

    public String getOrigin() {
        return origin;
    }

    public Date getImportedDate() {
        return importedDate;
    }
}
