#!/bin/bash

# Usage: ./monitor-sentris.sh <interval-seconds>
# Example: ./monitor-sentris.sh 2

if [ $# -ne 1 ]; then
  echo "Usage: $0 <interval-seconds>"
  exit 1
fi

INTERVAL=$1

mkdir -p docker-stats-logs

# Convert Docker human-readable sizes to bytes
convert_to_bytes() {
  local value=$1
  case "$value" in
    *KiB) awk "BEGIN{print ${value%KiB} * 1024}" ;;
    *MiB) awk "BEGIN{print ${value%MiB} * 1024 * 1024}" ;;
    *GiB) awk "BEGIN{print ${value%GiB} * 1024 * 1024 * 1024}" ;;
    *TiB) awk "BEGIN{print ${value%TiB} * 1024 * 1024 * 1024 * 1024}" ;;
    *kB)  awk "BEGIN{print ${value%kB} * 1000}" ;;
    *MB)  awk "BEGIN{print ${value%MB} * 1000 * 1000}" ;;
    *GB)  awk "BEGIN{print ${value%GB} * 1000 * 1000 * 1000}" ;;
    *B)   echo "${value%B}" ;;
    *)    echo "$value" ;;
  esac
}

echo "📊 Monitoring 'sentris' containers every $INTERVAL seconds..."
echo "CSV logs in ./docker-stats-logs/<container>.csv"
echo "Press Ctrl+C to stop."

while true; do
  docker ps --filter "name=sentris" --format "{{.ID}} {{.Names}}" | while read -r id name; do
    logfile="docker-stats-logs/${name}.csv"

    # Write CSV header if new
    if [ ! -f "$logfile" ]; then
      echo "timestamp,container_name,cpu_percent,mem_used_bytes,mem_total_bytes,net_in_bytes,net_out_bytes,block_in_bytes,block_out_bytes" > "$logfile"
    fi

    raw=$(docker stats --no-stream --format "{{.CPUPerc}},{{.MemUsage}},{{.NetIO}},{{.BlockIO}}" $id)

    cpu=$(echo "$raw" | cut -d',' -f1 | tr -d '%')
    mem=$(echo "$raw" | cut -d',' -f2)
    net=$(echo "$raw" | cut -d',' -f3)
    block=$(echo "$raw" | cut -d',' -f4)

    mem_used=$(echo "$mem" | awk '{print $1}')
    mem_total=$(echo "$mem" | awk '{print $3}')
    net_in=$(echo "$net" | awk '{print $1}')
    net_out=$(echo "$net" | awk '{print $3}')
    block_in=$(echo "$block" | awk '{print $1}')
    block_out=$(echo "$block" | awk '{print $3}')

    mem_used_b=$(convert_to_bytes "$mem_used")
    mem_total_b=$(convert_to_bytes "$mem_total")
    net_in_b=$(convert_to_bytes "$net_in")
    net_out_b=$(convert_to_bytes "$net_out")
    block_in_b=$(convert_to_bytes "$block_in")
    block_out_b=$(convert_to_bytes "$block_out")

    timestamp=$(date +"%Y-%m-%d %H:%M:%S")

    echo "$timestamp,$name,$cpu,$mem_used_b,$mem_total_b,$net_in_b,$net_out_b,$block_in_b,$block_out_b" >> "$logfile"
  done

  sleep "$INTERVAL"
done