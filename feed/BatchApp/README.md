# BatchApp

Spring Batch + Quartz application for periodic maintenance tasks.

## Features

### 1. Follow Count Sync Batch Job
- **Schedule**: Every hour at :00 (Quartz cron: `0 0 * * * ?`)
- **Job Name**: `followCountSyncJob`
- **Purpose**: Synchronizes following/follower counts from ScyllaDB to User table and Redis
- **Implementation**: Spring Batch job with Tasklet
- **Behavior**:
  - Counts followers and followings from `follower_by_user` and `following_by_user` tables
  - Updates the `user` table's `following_count` and `follower_count` columns
  - Updates Redis cache with the latest counts
  - **Error handling**: Redis failures are logged but don't stop the process
  - Continues processing all users even if individual users fail

### 2. News Feed Cache Cleanup Batch Job
- **Schedule**: Daily at midnight (Quartz cron: `0 0 0 * * ?`)
- **Job Name**: `newsFeedCleanupJob`
- **Purpose**: Maintains news feed cache size by keeping only the 50 most recent feeds
- **Implementation**: Spring Batch job with Tasklet
- **Behavior**:
  - Iterates through all `newsfeed:*` keys in Redis
  - Trims each zset to keep only the top 50 items (highest scores = most recent feeds)
  - Removes older feeds from the cache
  - Logs trimming statistics

## Architecture

### Technology Stack
- **Spring Boot** 3.4.5
- **Spring Batch** - Job orchestration and management
- **Quartz Scheduler** - Job scheduling and triggering
- **Spring Data Cassandra** - ScyllaDB access
- **Spring Data Redis** - Redis operations
- **Kotlin** 1.9.0

### Components

#### Batch Jobs
- `FollowCountSyncBatchJob`: Spring Batch job configuration for follow count sync
- `NewsFeedCleanupBatchJob`: Spring Batch job configuration for news feed cleanup

#### Quartz Integration
- `QuartzConfiguration`: Defines Quartz job details and triggers
- `BatchJobLauncher`: Bridges Quartz and Spring Batch, implements `QuartzJobBean`

#### Repositories
- `UserRepository`: ScyllaDB user table access
- `FollowingRepository`: Count followings from ScyllaDB
- `FollowerRepository`: Count followers from ScyllaDB
- `FollowRedisRepository`: Redis follow count operations
- `NewsFeedRedisRepository`: Redis news feed cache operations

## Configuration

```yaml
spring:
  batch:
    jdbc:
      initialize-schema: always
    job:
      enabled: false  # Prevent auto-run on startup
  quartz:
    job-store-type: memory
    properties:
      org:
        quartz:
          threadPool:
            threadCount: 5

batch:
  news-feed-cache:
    max-items: 50
  follow-count-sync:
    chunk-size: 100
```

## How It Works

1. **Quartz** triggers batch jobs according to cron schedules
2. **BatchJobLauncher** receives Quartz trigger and launches corresponding Spring Batch job
3. **Spring Batch** executes the job using Tasklet pattern
4. Job results and status are tracked by Spring Batch infrastructure
5. Logs provide detailed execution information

## Running

```bash
./gradlew :BatchApp:bootRun
```

## Job Management

Jobs are automatically scheduled on application startup. Each job execution includes:
- Unique job parameters (timestamp)
- Execution status tracking
- Detailed logging
- Error handling and recovery

## Notes

- Redis failures in follow count sync are non-critical and only logged
- All batch operations use reactive programming for better performance
- Quartz uses in-memory job store for simplicity (can be changed to JDBC for persistence)
- Job history is maintained by Spring Batch's job repository
