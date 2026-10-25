-- A small preview of each photo for the lists, stored next to the file in the same storage: in the database with the
-- content, in S3 as the object <key>.preview (deleted with the file). Not part of the backup, generated again.
-- NULL: not generated yet (e.g. the files before this version), NONE: no preview (PDFs, images Java cannot decode),
-- JPEG: a preview is stored.
ALTER TABLE attachment ADD COLUMN a_preview VARCHAR(10);
ALTER TABLE attachment_content ADD COLUMN ac_preview BYTEA;
UPDATE attachment SET a_preview = 'NONE' WHERE a_content_type = 'application/pdf';
