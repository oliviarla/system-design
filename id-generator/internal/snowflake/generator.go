package snowflake

import (
	"sync"
	"time"
)

const (
	epoch        = int64(1672531200000)
	nodeBits     = 10
	sequenceBits = 12
	maxNodeID    = -1 ^ (-1 << nodeBits)
	maxSequence  = -1 ^ (-1 << sequenceBits)
	timeShift    = nodeBits + sequenceBits
	nodeShift    = sequenceBits
)

type Generator struct {
	mu        sync.Mutex
	nodeID    int64
	timestamp int64
	sequence  int64
}

func NewGenerator(nodeID int64) *Generator {
	if nodeID > maxNodeID {
		panic("nodeID too large")
	}
	return &Generator{nodeID: nodeID}
}

func (g *Generator) Generate() int64 {
	g.mu.Lock()
	defer g.mu.Unlock()

	now := time.Now().UnixMilli()
	if now == g.timestamp { // If the timestamp hasn't changed, increment the sequence
		g.sequence = (g.sequence + 1) & maxSequence
		if g.sequence == 0 {
			for now <= g.timestamp {
				now = time.Now().UnixMilli()
			}
		}
	} else {
		g.sequence = 0
	}
	g.timestamp = now

	return ((now - epoch) << timeShift) |
		(g.nodeID << nodeShift) |
		(g.sequence)
}
