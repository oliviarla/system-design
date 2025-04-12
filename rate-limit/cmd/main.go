package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	limiter "rate-limit/pkg/rate-limiter"
	"strings"
	"time"

	"github.com/redis/go-redis/v9"
)

var rateLimiter *limiter.SlidingWindowCounterRateLimiter
var salt string

func rateLimitMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		idx := strings.LastIndex(r.RemoteAddr, ":")
		if idx == -1 {
			http.Error(w, "Cannot split ip.", http.StatusBadRequest)
			return
		}

		ip := r.RemoteAddr[:idx]
		clientKey := fmt.Sprintf("%s:/recipe", ip)

		ctx := context.Background()
		allowed, err := rateLimiter.IsAllowed(ctx, clientKey, salt)

		if err != nil {
			http.Error(w, "Rate limiter error", http.StatusInternalServerError)
			return
		}

		if !allowed {
			http.Error(w, "Too Many Requests", http.StatusTooManyRequests)
			log.Println("[INFO]: " + r.URL.Path + " failed")
			return
		}

		log.Println("[INFO]: " + r.URL.Path + " succeed")
		next.ServeHTTP(w, r)
	})
}

func recipeHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "Recipe requested: %s", r.URL.Path)
}

func main() {
	redisClient := redis.NewClient(&redis.Options{
		Addr: "localhost:6379",
		DB:   0,
	})

	rateLimiter = limiter.NewSlidingWindowCounterRateLimiter(
		redisClient,
		5,
		time.Minute,
		20*time.Second,
	)

	salt = os.Getenv("IP_HASH_SALT")

	mux := http.NewServeMux()
	mux.Handle("/recipe/", rateLimitMiddleware(http.HandlerFunc(recipeHandler)))

	log.Println("Server started on :8080")
	http.ListenAndServe(":8080", mux)
}
