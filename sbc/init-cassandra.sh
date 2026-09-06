#!/bin/bash
set -e

# Run the original Cassandra entrypoint in background
exec docker-entrypoint.sh "$@" &
CASSANDRA_PID=$!

# Wait for Cassandra to be ready
echo "Waiting for Cassandra to be ready..."
until cqlsh -e "SELECT now() FROM system.local" > /dev/null 2>&1; do
  sleep 2
done
echo "Cassandra is ready. Running schema.cql..."
cqlsh -f /docker-entrypoint-initdb.d/schema.cql
echo "Schema initialization complete."

wait $CASSANDRA_PID