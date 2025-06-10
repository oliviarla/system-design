CREATE KEYSPACE feed_app
    WITH replication = {
        'class': 'SimpleStrategy',
        'replication_factor': 1
        };

CREATE TABLE IF NOT EXISTS feed_app.user
(
    username text PRIMARY KEY,
    password text,
    created_at timestamp,
);

CREATE TABLE IF NOT EXISTS feed_app.feed
(
    feedId text PRIMARY KEY,
    password text,
    created_at timestamp,
);
