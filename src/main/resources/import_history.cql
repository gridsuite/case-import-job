CREATE TABLE IF NOT EXISTS import_history.files (
    filename text,
    origin text,
    import_date timestamp,
    PRIMARY KEY (filename, origin)
);
