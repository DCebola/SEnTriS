import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import glob
import os
import argparse

# -------------------------------
# Parse arguments
# -------------------------------
parser = argparse.ArgumentParser(description="Plot SEnTriS Docker stats logs.")
parser.add_argument(
    "--resample",
    type=str,
    default=None,
    help="Optional Pandas resample rule (e.g., '10S' for 10 seconds, '1T' for 1 minute)."
)
args = parser.parse_args()

# -------------------------------
# Setup
# -------------------------------
os.makedirs("plots", exist_ok=True)
files = glob.glob("docker-stats-logs/*.csv")

dfs = []
for file in files:
    container_name = os.path.basename(file).replace(".csv", "")
    df = pd.read_csv(file, parse_dates=["timestamp"])
    df["container_name"] = container_name
    dfs.append(df)

if not dfs:
    raise RuntimeError("No CSV log files found in docker-stats-logs/")

data = pd.concat(dfs)

# -------------------------------
# Optional resampling
# -------------------------------
if args.resample:
    print(f"⏱ Resampling data with rule: {args.resample}")
    # Group by container and resample
    data = (
        data.set_index("timestamp")
        .groupby("container_name")
        .resample(args.resample)
        .mean(numeric_only=True)
        .dropna()
        .reset_index()
    )

# -------------------------------
# Formatter for human-readable bytes
# -------------------------------
def human_readable_bytes(x, pos):
    for unit in ["B", "KB", "MB", "GB", "TB"]:
        if abs(x) < 1024.0:
            return f"{x:.1f} {unit}"
        x /= 1024.0
    return f"{x:.1f} PB"

formatter = ticker.FuncFormatter(human_readable_bytes)

# -------------------------------
# Plot CPU %
# -------------------------------
plt.figure(figsize=(10, 6))
for name, group in data.groupby("container_name"):
    plt.plot(group["timestamp"], group["cpu_percent"], label=name)
plt.title("CPU Usage (%)")
plt.xlabel("Time")
plt.ylabel("CPU %")
plt.legend()
plt.grid(True)
plt.tight_layout()
plt.savefig("plots/cpu_usage.png")

# -------------------------------
# Plot Memory
# -------------------------------
plt.figure(figsize=(10, 6))
for name, group in data.groupby("container_name"):
    plt.plot(group["timestamp"], group["mem_used_bytes"], label=name)
plt.title("Memory Usage")
plt.xlabel("Time")
plt.ylabel("Memory")
plt.gca().yaxis.set_major_formatter(formatter)
plt.legend()
plt.grid(True)
plt.tight_layout()
plt.savefig("plots/memory_usage.png")

# -------------------------------
# Plot Network I/O
# -------------------------------
plt.figure(figsize=(10, 6))
for name, group in data.groupby("container_name"):
    plt.plot(group["timestamp"], group["net_in_bytes"], label=f"{name} (in)")
    plt.plot(group["timestamp"], group["net_out_bytes"], label=f"{name} (out)")
plt.title("Network I/O")
plt.xlabel("Time")
plt.ylabel("Bytes")
plt.gca().yaxis.set_major_formatter(formatter)
plt.legend()
plt.grid(True)
plt.tight_layout()
plt.savefig("plots/network_io.png")

# -------------------------------
# Plot Block I/O
# -------------------------------
plt.figure(figsize=(10, 6))
for name, group in data.groupby("container_name"):
    plt.plot(group["timestamp"], group["block_in_bytes"], label=f"{name} (in)")
    plt.plot(group["timestamp"], group["block_out_bytes"], label=f"{name} (out)")
plt.title("Block I/O")
plt.xlabel("Time")
plt.ylabel("Bytes")
plt.gca().yaxis.set_major_formatter(formatter)
plt.legend()
plt.grid(True)
plt.tight_layout()
plt.savefig("plots/block_io.png")

print("✅ Multi-line plots saved in ./plots/")