## Introduction
This project is an example of Redis cache implementation with Spring Boot.

## Run the project
### Redis
You will need to launch a Redis instance on your computer before running the project.

You can either install Redis directly on your machine or run it through Docker :
```
docker run -p 6379:6379 redis
```

### Application
Once Redis has started, you can launch the Spring Boot project and try it out.

Get all movies
```
curl --request GET \
  --url http://localhost:8090/redis-cache-example/movies
```

Get a movie by id
```
curl --request GET \
  --url http://localhost:8090/redis-cache-example/movies/24
```

Add a movie to cache
```
curl --request POST \
  --url http://localhost:8090/redis-cache-example/movies \
  --header 'content-type: application/json' \
  --data '{
  "id": 28,
  "title": "Some title",
  "release_date": "2022-02-04"
}'
```

Clear all cache
```
curl --request DELETE \
  --url http://localhost:8090/redis-cache-example/movies/cache
```

Clean a movie from cache
```
curl --request DELETE \
  --url http://localhost:8090/redis-cache-example/movies/28/cache
```

Health check
```
curl --request GET \
  --url http://localhost:8090/redis-cache-example/actuator/health
```

## Redis
### redis-cli
You can use redis-cli to manipulate the cache directly.

If you started redis with docker, you can connect to the container and use redis-cli
```
docker exec -it <container_name> redis-cli
```

Once connected on redis-cli, you can use some useful commands:

Get all keys
```
keys *
```

Search a key (* can be use as wildcard)
```
keys <pattern>
```

Get a value
```
get <key>
```

Add a key/value
```
set <key> <value> [ex <time_in_seconds> | px <expiration_in_milliseconds>]
```

Get the TTL (time to live)
It returns the TTL or -1 if the TTL infinite.
If the entry doesn't exist, it returns -2
```
ttl <key>
```

Persist an entry (infinite TTL)
```
persist <key>
```

Update the TTL for an entry
```
expire <key> <time_in_seconds>
```

Remove an entry
```
del <key>
```

Select a different database on the cache
```
select <index>
```

Move an entry from a database to another
```
move <key> <target_index>
```

Move an entry from a database to another
```
move <key> <target_index>
```

Count the number of entry in the current database
```
dbsize
```

Clear the current database
```
flushdb
```

Clear all
```
flushall
```

## Evict policy configuration
You can configure the delete policy to apply when the cache is full to avoid errors.

With volatile options, only non-persistent entries get deleted (with infinite TTL)
The allkeys options are the same as volatile but they will also delete persistent entries
- noeviction: no deletion, it will lead to an error when the cache is full
- volatile-ttl: delete the entries with the shortest TTL left
- volatile-lru / allkeys-lru: delete the entries that have been the less recently used
- volatile-lfu / allkeys-lfu: delete the entries that have been the less frequently used
- volatile-random / allkeys-random: delete the entries randomly

For volatile options, if you are using persistent entries, it can lead to an error since those entries won't be deleted.

You can retrieve the current applied policy on redis-cli
```
config get maxmemory-policy
```

You can also update the policy
```
config set maxmemory-policy <policy>
```
