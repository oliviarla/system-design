# Rate Limiter

## Introduction
- Go based Rate Limiter with Sliding Window Counter Algorithm
- This program limits when lots of requests come into the server, only permitted count of requests will be processed.
- This limits http requests which matches `/recipe/{id}`.
- Each user is distinguished by IP address.

## Main Feature
- Redis
  - To support distribution system, store the request count data into Redis.
  - Use Hashes data structure. One Hash item per user(IP).
  - Key of the entry is <time>, and value of the entry is `<request count>`.
  - By using Redis, Rate Limiter can be scaled out. So there's no SPoF.

```
- key: rate_limit:<hashed IP by SHA256>
- hkey: <unix time> / <time sub window size>
- hvalue: <count of time range>
```

##  References
  - https://github.com/raphaeldelio/redis-rate-limiter-kotlin-example
