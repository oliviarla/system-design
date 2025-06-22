package httpserver

import (
	"fmt"
	"id-generator/internal/snowflake"
	"net/http"
)

var generator = snowflake.NewGenerator(1)

func NewRouter() *http.ServeMux {
	mux := http.NewServeMux()
	mux.HandleFunc("/generate", generateHandler)
	return mux
}

func generateHandler(w http.ResponseWriter, r *http.Request) {
	id := generator.Generate()
	fmt.Fprintf(w, "%d", id)
}
