CREATE TABLE IF NOT EXISTS acquisition_logs.files (
    filename text,
    origin text,
    acq_date timestamp,
    PRIMARY KEY (filename, origin)
);
