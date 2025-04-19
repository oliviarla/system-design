package main

import (
	"context"
	"crypto/md5"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/jxskiss/base62"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

var collection *mongo.Collection

func init() {
	client, err := mongo.Connect(context.TODO(), options.Client().ApplyURI("mongodb://localhost:27017"))
	if err != nil {
		panic(err)
	}
	collection = client.Database("urlshortener").Collection("urls")
}

type URLMapping struct {
	Hash      string    `bson:"hash"`
	URL       string    `bson:"url"`
	ExpiresAt time.Time `bson:"expiresAt,omitempty"`
}
type ShortenRequest struct {
	URL    string `json:"url"`
	Expire int64  `json:"expire"` // 초 단위 (0이면 영구 저장)
}

func shortenURLHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	body, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "Invalid Body", http.StatusBadRequest)
		return
	}
	defer r.Body.Close()

	var req ShortenRequest
	if err := json.Unmarshal(body, &req); err != nil || req.URL == "" || req.Expire < 0 {
		http.Error(w, "Invalid JSON or expire time", http.StatusBadRequest)
		return
	}

	var expireTime time.Time
	if req.Expire > 0 {
		expireTime = time.Now().Add(time.Duration(req.Expire) * time.Second)
	}

	ctx := context.TODO()
	var existing URLMapping
	err = collection.FindOne(ctx, bson.M{"url": req.URL}).Decode(&existing)
	if err == nil && (existing.ExpiresAt.IsZero() || existing.ExpiresAt.After(time.Now())) {
		shortURL := fmt.Sprintf("http://localhost:8080/%s", existing.Hash)
		json.NewEncoder(w).Encode(map[string]string{"short_url": shortURL})
		return
	}

	var hash string
	for i := 0; i < 5; i++ {
		data := []byte(fmt.Sprintf("%s-%d", req.URL, i))
		sum := md5.Sum(data)
		encoded := base62.StdEncoding.EncodeToString(sum[:])
		hash = encoded[:8]

		var temp URLMapping
		err := collection.FindOne(ctx, bson.M{"hash": hash}).Decode(&temp)
		if err == mongo.ErrNoDocuments {
			break
		}
		if temp.URL == req.URL {
			break
		}
	}

	doc := URLMapping{
		Hash:      hash,
		URL:       req.URL,
		ExpiresAt: expireTime,
	}
	_, err = collection.InsertOne(ctx, doc)
	if err != nil {
		http.Error(w, "Database Error", http.StatusInternalServerError)
		return
	}

	shortURL := fmt.Sprintf("http://localhost:8080/%s", hash)
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"short_url": shortURL})
}

func redirectURLHandler(w http.ResponseWriter, r *http.Request) {
	hash := strings.TrimPrefix(r.URL.Path, "/")
	if hash == "" {
		http.NotFound(w, r)
		return
	}

	ctx := context.TODO()
	var result URLMapping
	err := collection.FindOne(ctx, bson.M{"hash": hash}).Decode(&result)
	if err != nil {
		http.NotFound(w, r)
		return
	}

	if !result.ExpiresAt.IsZero() && result.ExpiresAt.Before(time.Now()) {
		collection.DeleteOne(ctx, bson.M{"hash": hash})
		http.Error(w, "URL expired", http.StatusGone)
		return
	}

	http.Redirect(w, r, result.URL, http.StatusFound)
}

func main() {
	http.HandleFunc("/shorten", shortenURLHandler)
	http.HandleFunc("/", redirectURLHandler)

	fmt.Println("Server running at :8080")
	if err := http.ListenAndServe(":8080", nil); err != nil {
		panic(err)
	}
}
