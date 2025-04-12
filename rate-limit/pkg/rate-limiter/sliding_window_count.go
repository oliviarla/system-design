package rate_limiter

import (
	"fmt"
	"rate-limit/pkg/hash"
	"strconv"
	"time"

	"github.com/redis/go-redis/v9"
	"golang.org/x/net/context"
)

type SlidingWindowCounterRateLimiter struct {
	RedisClient   *redis.Client
	Limit         int
	WindowSize    time.Duration // total window size (e.g. 1 minute)
	SubWindowSize time.Duration // sub-window size (e.g. 10 seconds)
	NumSubWindows int64
}

func NewSlidingWindowCounterRateLimiter(client *redis.Client, limit int, windowSize, subWindowSize time.Duration) *SlidingWindowCounterRateLimiter {
	return &SlidingWindowCounterRateLimiter{
		RedisClient:   client,
		Limit:         limit,
		WindowSize:    windowSize,
		SubWindowSize: subWindowSize,
		NumSubWindows: windowSize.Milliseconds() / subWindowSize.Milliseconds(),
	}
}

func (r *SlidingWindowCounterRateLimiter) IsAllowed(ctx context.Context, clientId string, salt string) (bool, error) {
	now := time.Now().UnixMilli()
	subWindowSizeMillis := r.SubWindowSize.Milliseconds()
	numSubWindows := r.WindowSize.Milliseconds() / subWindowSizeMillis

	key := fmt.Sprintf("rate_limit:%s", hash.Hash(clientId, salt))
	currentSubWindow := now / subWindowSizeMillis
	subWindowStr := strconv.FormatInt(currentSubWindow, 10)

	pipe := r.RedisClient.Pipeline()
	pipe.HIncrBy(ctx, key, subWindowStr, 1)
	pipe.HExpireWithArgs(ctx, key, time.Second*60, redis.HExpireArgs{NX: true}, subWindowStr)

	subWindowKeys := make([]string, 0, numSubWindows)
	for i := currentSubWindow; i > currentSubWindow-int64(numSubWindows); i-- {
		subWindowKeys = append(subWindowKeys, strconv.FormatInt(i, 10))
	}

	hgetCmds := make([]*redis.StringCmd, len(subWindowKeys))
	for i, subKey := range subWindowKeys {
		hgetCmds[i] = pipe.HGet(ctx, key, subKey)
	}

	_, err := pipe.Exec(ctx)
	if err != nil && err != redis.Nil {
		return false, err
	}

	total := 0
	for _, cmd := range hgetCmds {
		val, err := cmd.Result()
		if err == redis.Nil {
			continue
		} else if err != nil {
			return false, err
		}
		if count, convErr := strconv.Atoi(val); convErr == nil {
			total += count
		}
	}

	return total <= r.Limit, nil
}
