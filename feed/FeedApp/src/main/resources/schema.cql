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

CREATE TABLE IF NOT EXISTS feed_app.following_by_user (
    username text,
    following_username text,
    followed_at timestamp,
    PRIMARY KEY (username, following_username)
);

CREATE TABLE IF NOT EXISTS feed_app.follower_by_user (
    username text,
    follower_username text,
    followed_at timestamp,
    PRIMARY KEY (username, follower_username)
);

CREATE TABLE IF NOT EXISTS feed_app.feed
(
    id text PRIMARY KEY,
    content text,
    username text,
    created_at timestamp,
    updated_at timestamp,
);
