package hash

import (
	"crypto/sha256"
	"encoding/hex"
	"hash"
	"sync"
)

var hashPool = sync.Pool{
	New: func() any {
		return sha256.New()
	},
}

func Hash(s string, salt string) string {
	h := hashPool.Get().(hash.Hash)
	defer hashPool.Put(h)

	h.Reset()
	h.Write([]byte(s + salt))
	sum := h.Sum(nil)

	return hex.EncodeToString(sum)
}
