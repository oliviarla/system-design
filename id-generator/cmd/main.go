package main

import (
	"fmt"
	"log"
	"net/http"
	"os"
	"strconv"
	"time"

	"id-generator/internal/snowflake"
)

func main() {
	nodeIdStr := os.Getenv("NODE_ID")
	nodeId, err := strconv.ParseInt(nodeIdStr, 10, 64)
	if err != nil {
		log.Fatalf("Invalid NODE_ID: %v", err)
	}

	generator := snowflake.NewGenerator(nodeId)

	http.HandleFunc("/generate", func(w http.ResponseWriter, r *http.Request) {
		id := generator.Generate()
		fmt.Fprintf(w, "%d", id)
	})

	http.HandleFunc("/max", func(w http.ResponseWriter, r *http.Request) {
		ms, err := validateTime(r.URL.Query().Get("time"))
		if err != nil {
			w.WriteHeader(http.StatusBadRequest)
			fmt.Fprint(w, err)
			return
		}
		maxId := generator.MaxID(ms)
		fmt.Fprintf(w, "%d", maxId)
	})

	http.HandleFunc("/min", func(w http.ResponseWriter, r *http.Request) {
		ms, err := validateTime(r.URL.Query().Get("time"))
		if err != nil {
			w.WriteHeader(http.StatusBadRequest)
			fmt.Fprint(w, err)
			return
		}
		minId := generator.MinID(ms)
		fmt.Fprintf(w, "%d", minId)
	})

	log.Println("Server started on :9713")
	log.Fatal(http.ListenAndServe(":9713", nil))
}

func validateTime(ms string) (int64, error) {
	if ms == "" {
		return 0, fmt.Errorf("missing time query param")
	}
	timeInMillis, err := strconv.ParseInt(ms, 10, 64)
	if err != nil || timeInMillis < time.Unix(0, 0).UnixMilli() {
		return 0, fmt.Errorf("invalid time: %v", err)
	}
	return timeInMillis, nil
}
