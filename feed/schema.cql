CREATE KEYSPACE user_app
    WITH replication = {
        'class': 'SimpleStrategy',
        'replication_factor': 1
        };

CREATE KEYSPACE feed_app
    WITH replication = {
        'class': 'SimpleStrategy',
        'replication_factor': 1
        };

CREATE TABLE IF NOT EXISTS user_app.user
(
    username text PRIMARY KEY,
    password text,
    created_at timestamp,
    following_count int,
    follower_count int
);

CREATE TABLE IF NOT EXISTS user_app.following_by_user (
    username text,
    following_username text,
    followed_at timestamp,
    PRIMARY KEY (username, following_username)
);

CREATE TABLE IF NOT EXISTS user_app.follower_by_user (
    username text,
    follower_username text,
    followed_at timestamp,
    PRIMARY KEY (username, follower_username)
);

CREATE TABLE IF NOT EXISTS feed_app.feeds_by_user (
    username text,
    created_at timestamp,
    feed_id text,
    PRIMARY KEY (username, feed_id)
) WITH CLUSTERING ORDER BY (feed_id DESC);

CREATE TABLE IF NOT EXISTS feed_app.feeds_by_id (
    feed_id bigint PRIMARY KEY,
    username text,
    created_at timestamp,
    updated_at timestamp,
    content text
);
