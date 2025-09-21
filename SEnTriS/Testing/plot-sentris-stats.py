#!/usr/bin/env python3
"""
plot-sentris-stats.py

Generates per-container and combined plots of Docker stats logs.

Usage:
  python plot-sentris-stats.py --resample 5min --yscale linear --start 0 --end 120
"""

import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np
import glob
import os
import argparse

# -------------------------------
# Parse arguments
# -------------------------------
parser = argparse.ArgumentParser(description="Analyze Docker stats logs and generate plots.")
parser.add_argument(
    "--resample",
    type=str,
    default=None,
    help="Optional Pandas resample rule (e.g., '10S' for 10 seconds, '5min' for 5 minutes)."
)
parser.add_argument(
    "--yscale",
    type=str,
    choices=["linear", "log", "sqrt"],
    default="linear",
    help="Y-axis scale to use (linear, log, sqrt)."
)
parser.add_argument(
    "--start",
    type=float,
    default=None,
    help="Optional start time in minutes since start (e.g., 0)."
)
parser.add_argument(
    "--end",
    type=float,
    default=None,
    help="Optional end time in minutes since start (e.g., 120)."
)
args = parser.parse_args()

# -------------------------------
# Helpers
# -------------------------------
def human_readable_bytes(x, pos=None):
    try:
        val = float(x)
    except Exception:
        return ""
    sign = "-" if val < 0 else ""
    val = abs(val)
    for unit in ["B", "KB", "MB", "GB", "TB", "PB"]:
        if val < 1024.0:
            if unit == "B":
                return f"{sign}{int(val)} {unit}"
            else:
                return f"{sign}{val:.1f} {unit}"
        val /= 1024.0
    return f"{sign}{val:.1f} EB"

def apply_yscale(ax):
    if args.yscale == "log":
        ax.set_yscale("log")
    elif args.yscale == "sqrt":
        from matplotlib import scale as mscale, transforms as mtransforms
        class SqrtScale(mscale.ScaleBase):
            name = "sqrt"
            def __init__(self, axis, **kwargs):
                super().__init__(axis)
                self._transform = self.SqrtTransform()
            def get_transform(self):
                return self._transform
            def set_default_locators_and_formatters(self, axis):
                axis.set_major_locator(ticker.AutoLocator())
                axis.set_major_formatter(ticker.ScalarFormatter())
            class SqrtTransform(mtransforms.Transform):
                input_dims = output_dims = 1
                def transform_non_affine(self, values):
                    values = np.array(values)
                    return np.sqrt(np.clip(values, 0, None))
                def inverted(self):
                    return SqrtScale.InvertedSqrtTransform()
            class InvertedSqrtTransform(mtransforms.Transform):
                input_dims = output_dims = 1
                def transform_non_affine(self, values):
                    return values**2
                def inverted(self):
                    return SqrtScale.SqrtTransform()
        mscale.register_scale(SqrtScale)
        ax.set_yscale("sqrt")
    else:
        ax.set_yscale("linear")

# -------------------------------
# Load data
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

data = pd.concat(dfs, ignore_index=True)

# Normalize time axis
start_time = data["timestamp"].min()
data["elapsed_min"] = (data["timestamp"] - start_time).dt.total_seconds() / 60.0

# Apply optional time window
if args.start is not None:
    data = data[data["elapsed_min"] >= args.start]
if args.end is not None:
    data = data[data["elapsed_min"] <= args.end]

# Optional resample
if args.resample:
    print(f"⏱ Resampling data with rule: {args.resample}")
    resampled = []
    for name, group in data.groupby("container_name"):
        g = (
            group.set_index("timestamp")
            .resample(args.resample)
            .mean(numeric_only=True)
            .dropna()
            .reset_index()
        )
        g["container_name"] = name
        g["elapsed_min"] = (g["timestamp"] - start_time).dt.total_seconds() / 60.0
        resampled.append(g)
    data = pd.concat(resampled, ignore_index=True)

# -------------------------------
# Plotting function
# -------------------------------
formatter = ticker.FuncFormatter(human_readable_bytes)

def plot_stat(df, col, title, ylabel, folder, use_formatter=False, combined=True):
    os.makedirs(folder, exist_ok=True)

    # per-container plots
    for name, group in df.groupby("container_name"):
        plt.figure(figsize=(10, 5))
        ax = plt.gca()
        group = group.sort_values("elapsed_min")

        if col not in group.columns or group[col].dropna().empty:
            plt.close()
            continue

        ax.plot(group["elapsed_min"], group[col], linestyle="-")
        plt.title(f"{title} - {name}")
        plt.xlabel("Time (minutes since start)")
        plt.ylabel(ylabel)
        if use_formatter:
            ax.yaxis.set_major_formatter(formatter)
        else:
            ax.yaxis.set_major_formatter(ticker.ScalarFormatter())
            ax.ticklabel_format(style="plain", axis="y")
        apply_yscale(ax)
        plt.grid(True)
        plt.tight_layout()

        outfile = os.path.join(folder, f"{name}_{col}.png")
        plt.savefig(outfile)
        plt.close()

    # combined plot
    if combined:
        plt.figure(figsize=(10, 5))
        ax = plt.gca()
        for name, group in df.groupby("container_name"):
            group = group.sort_values("elapsed_min")
            ax.plot(group["elapsed_min"], group[col], label=name, linestyle="-")
        plt.title(f"{title} - All Containers")
        plt.xlabel("Time (minutes since start)")
        plt.ylabel(ylabel)
        if use_formatter:
            ax.yaxis.set_major_formatter(formatter)
        else:
            ax.yaxis.set_major_formatter(ticker.ScalarFormatter())
            ax.ticklabel_format(style="plain", axis="y")
        apply_yscale(ax)
        plt.grid(True)
        plt.legend(bbox_to_anchor=(1.02, 1), loc="upper left", fontsize="small")
        plt.tight_layout()

        outfile = os.path.join(folder, f"all_{col}.png")
        plt.savefig(outfile)
        plt.close()

# -------------------------------
# Generate plots
# -------------------------------
plot_stat(data, "cpu_percent", "CPU Usage (%)", "CPU %", "plots/stats/cpu_usage")
plot_stat(data, "mem_used_bytes", "Memory Usage", "Memory", "plots/stats/memory_usage", use_formatter=True)
plot_stat(data, "net_in_bytes", "Network I/O (In)", "Bytes", "plots/stats/network_io", use_formatter=True)
plot_stat(data, "net_out_bytes", "Network I/O (Out)", "Bytes", "plots/stats/network_io", use_formatter=True)
plot_stat(data, "block_in_bytes", "Block I/O (In)", "Bytes", "plots/stats/block_io", use_formatter=True)
plot_stat(data, "block_out_bytes", "Block I/O (Out)", "Bytes", "plots/stats/block_io", use_formatter=True)

print("✅ Per-container and combined plots saved under ./plots/<stat-type>/")
