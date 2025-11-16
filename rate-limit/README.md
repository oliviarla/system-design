# Rate Limiter

## Introduction
- Go based Rate Limiter example with Sliding Window Counter Algorithm
- This program limits when lots of requests come into the server, only permitted count of requests will be processed.
- This limits http requests which matches `/recipe/{id}`.
- Each user is distinguished by IP address.

## Main Feature
- Redis
  - To support distribution system, store the request count data into Redis.
  - Use Hashes data structure. One Hash item per user(IP).
  - Key of the entry is <time>, and value of the entry is `<request count>`.

```
- key: rate_limit:<hashed IP by SHA256>
- hkey: <unix time> / <time sub window size>
- hvalue: <count of time range>
```

## Description
### Add Request Count
- The `HINCRBY` command is used to add fields in a Redis Hash.
- The value `unix time (ms) / 20000(ms)` remains the same for any timestamp within the same 20-second range.
- Therefore, this value is used for hash key.
- For example, when using 20 seconds as the subwindow, all requests from 3:00 to 3:20 will be counted under the same hash field using `INCR`.
- The `HEXPIRE` command ensures that each Redis Hash field remains valid only for 1 minute from the moment it is created.
- After 1 minute, the corresponding subwindow is no longer needed, so it is ideal to have the field automatically removed.

### Check Request Available
- Each hkey differs by 1 per subwindow.
- Therefore, to determine whether a request is allowed, the system must look at all subwindow fields that fall within the current 1-minute time window.
- Using `HMGET`, the values of all relevant fields can be fetched; if their total is below the limit, the request is permitted.

### Is HEXPIRE Necessary?
- It is possible to implement the logic without using HEXPIRE, but the code becomes more complex.
- If all subwindows are stored in a single hash, the application must periodically remove expired subwindow fields using HDEL.
- By using HEXPIRE, Redis will automatically expire fields, preventing situations where invalid subwindows remain because HDEL failed due to temporary network issues or else.

##  References
  - https://github.com/raphaeldelio/redis-rate-limiter-kotlin-example
