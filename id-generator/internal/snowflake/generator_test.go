package snowflake_test

import (
	"id-generator/internal/snowflake"
	"testing"
	"time"
)

func TestGenerator_Generate(t *testing.T) {
	tests := []struct {
		name string // description of this test case
		// Named input parameters for receiver constructor.
		nodeID int64
		want   int64
	}{
		// TODO : Add test cases.
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			g := snowflake.NewGenerator(tt.nodeID)
			got := g.Generate()
			// TODO: update the condition below to compare got with tt.want.
			if got < tt.want {
				t.Errorf("Generate() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestGenerator_MaxID(t *testing.T) {
	g := snowflake.NewGenerator(1)
	expectIdBefore := g.Generate()
	tests := []struct {
		name string
		time int64
		want bool
	}{
		{
			name: "generate_max_id",
			time: time.Now().Add(1 * time.Second).UnixMilli(),
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := g.MaxID(tt.time)
			if (got >= expectIdBefore) != tt.want {
				t.Errorf("MaxID() = %v, want before than %v", got, expectIdBefore)
			}
		})
	}
}

func TestDifferentGenerator_MaxID(t *testing.T) {
	g := snowflake.NewGenerator(1)
	expectIdBefore := g.Generate()
	tests := []struct {
		name string
		time int64
		want bool
	}{
		{
			name: "generate_max_id",
			time: time.Now().Add(1 * time.Second).UnixMilli(),
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			g := snowflake.NewGenerator(100)
			got := g.MaxID(tt.time)
			if (got >= expectIdBefore) != tt.want {
				t.Errorf("MaxID() = %v, want before than %v", got, expectIdBefore)
			}
		})
	}
}

func TestGenerator_MinID(t *testing.T) {
	t.Run("generate_min_id", func(t *testing.T) {
		g := snowflake.NewGenerator(1)
		got := g.MinID(time.Now().UnixMilli())
		newId := g.Generate()
		if got >= newId {
			t.Errorf("MinID() = %v, want below %v", got, newId)
		}
	})
}
