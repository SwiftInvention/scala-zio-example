-- Example fixtures for the template's smoke test. Delete or replace before production use.
-- These are deliberately scoped to the @example.test domain (RFC 6761) so it's obvious
-- they're not real users.

INSERT INTO customer (id, email, name) VALUES
    ('c-001', 'ada@example.test',   'Ada Lovelace'),
    ('c-002', 'alan@example.test',  'Alan Turing'),
    ('c-003', 'grace@example.test', 'Grace Hopper');
