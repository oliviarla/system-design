# Dockerfile
FROM golang:1.24.4-alpine3.21

WORKDIR /app

COPY . .

RUN go build -o snowflake ./cmd

ENV NODE_ID=0

CMD ["./snowflake"]
