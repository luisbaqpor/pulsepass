-- V3: URL de streaming opcional para eventos híbridos (FR-EVT-006)
ALTER TABLE events ADD COLUMN streaming_url VARCHAR(500);