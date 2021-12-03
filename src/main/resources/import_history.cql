CREATE TABLE IF NOT EXISTS files (
    filename text,
    origin text,
    import_date timestamp,
    PRIMARY KEY (filename, origin)
);
