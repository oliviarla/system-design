package main

import (
	"fmt"
	"log"
	"net/http"
	"os"
	"strconv"

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

	log.Println("Server started on :9713")
	log.Fatal(http.ListenAndServe(":9713", nil))
}
