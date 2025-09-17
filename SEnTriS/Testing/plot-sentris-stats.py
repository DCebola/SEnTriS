#!/usr/bin/env python3
"""
plot-sentris-stats.py

Usage examples:
  python plot-sentris-stats.py --resample 5min --yscale linear
  python plot-sentris-stats.py --resample 10S --yscale sqrt
"""

import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np
import glob
import os
import argparse
import matplotlib.scale as mscale
import matplotlib.transforms as mtransforms

PATH="plots/stats"

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
args = parser.parse_args()

# -------------------------------
# Helpers
# -------------------------------
def human_readable_bytes(x, pos=None):
    """Format bytes as human readable string (used both as formatter and direct call)."""
    try:
        val = float(x)
    except Exception:
        return ""
    # handle very small or negative gracefully
    sign = "-" if val < 0 else ""
    val = abs(val)
    for unit in ["B", "KB", "MB", "GB", "TB", "PB"]:
        if val < 1024.0:
            # show one decimal for larger units, integer for bytes
            if unit == "B":
                return f"{sign}{int(val)} {unit}"
            else:
                return f"{sign}{val:.1f} {unit}"
        val /= 1024.0
    return f"{sign}{val:.1f} EB"

def format_value_for_metric(metric, val):
    """Return a human-readable string for annotation based on metric name."""
    if pd.isna(val):
        return ""
    if "bytes" in metric or any(k in metric for k in ("net_", "block_", "mem_")):
        return human_readable_bytes(val)
    if "cpu" in metric:
        return f"{val:.1f} %"
    # fallback numeric formatting
    try:
        return f"{val:.3g}"
    except Exception:
        return str(val)

# -------------------------------
# Setup / load CSVs
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

# -------------------------------
# Normalize time axis (we keep original timestamp as well)
# -------------------------------
start_time = data["timestamp"].min()
data["elapsed_sec"] = (data["timestamp"] - start_time).dt.total_seconds()
data["elapsed_min"] = data["elapsed_sec"] / 60.0

# -------------------------------
# Optional resampling (preserve container_name)
# -------------------------------
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
        resampled.append(g)
    data = pd.concat(resampled, ignore_index=True)

    # recompute elapsed fields after resample
    start_time = data["timestamp"].min()
    data["elapsed_sec"] = (data["timestamp"] - start_time).dt.total_seconds()
    data["elapsed_min"] = data["elapsed_sec"] / 60.0

# -------------------------------
# Custom sqrt scale (fixed constructor) and register
# -------------------------------
class SqrtScale(mscale.ScaleBase):
    name = "sqrt"

    def __init__(self, axis, **kwargs):
        super().__init__(axis)
        self.transform = self.SqrtTransform()

    def get_transform(self):
        return self.transform

    def set_default_locators_and_formatters(self, axis):
        axis.set_major_locator(ticker.AutoLocator())
        axis.set_major_formatter(ticker.ScalarFormatter())

    class SqrtTransform(mtransforms.Transform):
        input_dims = output_dims = 1

        def transform_non_affine(self, values):
            values = np.array(values)
            # clip negatives to 0 before sqrt so transform is real-valued
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

def apply_yscale(ax):
    if args.yscale == "log":
        ax.set_yscale("log")
    elif args.yscale == "sqrt":
        ax.set_yscale("sqrt")
    else:
        ax.set_yscale("linear")

# -------------------------------
# Formatter for human-readable bytes (tick labels)
# -------------------------------
formatter = ticker.FuncFormatter(lambda x, pos: human_readable_bytes(x))

# -------------------------------
# Line / marker styles
# -------------------------------
linestyles = ["-", "--", "-.", ":"]
markers = ["o", "s", "D", "^", "v", "<", ">", "x", "+", "*"]

def get_style(idx):
    return linestyles[idx % len(linestyles)], markers[idx % len(markers)]

# -------------------------------
# Summary collector (we will store numeric values and a readable string)
# -------------------------------
summary = []  # rows: [container_name, metric, type (max/min), value (numeric), value_hr (str), at_minute]

# -------------------------------
# Plot helper (adds readable annotations + collects summary)
# -------------------------------
def plot_stat(df, columns, title, ylabel, filename, use_formatter=False):
    plt.figure(figsize=(12, 6))
    ax = plt.gca()
    for idx, (name, group) in enumerate(df.groupby("container_name")):
        ls, marker = get_style(idx)
        # ensure group sorted by elapsed_min
        group = group.sort_values("elapsed_min")
        if isinstance(columns, list):
            for col in columns:
                if col not in group.columns or group[col].dropna().empty:
                    continue
                ax.plot(
                    group["elapsed_min"], group[col],
                    linestyle=ls, marker=marker, markersize=3,
                    label=f"{name} ({col.replace('_bytes','')})"
                )
                # annotate max & min (human readable)
                try:
                    max_idx = group[col].idxmax()
                    min_idx = group[col].idxmin()
                except Exception:
                    max_idx = min_idx = None

                if pd.notna(max_idx):
                    max_row = group.loc[max_idx]
                    val = float(max_row[col])
                    val_hr = format_value_for_metric(col, val)
                    ax.annotate(
                        val_hr,
                        (max_row["elapsed_min"], max_row[col]),
                        textcoords="offset points", xytext=(0,6), ha="center",
                        fontsize=8, bbox=dict(facecolor="white", alpha=0.75, edgecolor="none")
                    )
                    summary.append([name, col, "max", val, val_hr, float(max_row["elapsed_min"])])

                if pd.notna(min_idx):
                    min_row = group.loc[min_idx]
                    val = float(min_row[col])
                    val_hr = format_value_for_metric(col, val)
                    ax.annotate(
                        val_hr,
                        (min_row["elapsed_min"], min_row[col]),
                        textcoords="offset points", xytext=(0,-10), ha="center",
                        fontsize=8, bbox=dict(facecolor="white", alpha=0.75, edgecolor="none")
                    )
                    summary.append([name, col, "min", val, val_hr, float(min_row["elapsed_min"])])
        else:
            col = columns
            if col not in group.columns or group[col].dropna().empty:
                continue
            ax.plot(
                group["elapsed_min"], group[col],
                linestyle=ls, marker=marker, markersize=3,
                label=name
            )
            try:
                max_idx = group[col].idxmax()
                min_idx = group[col].idxmin()
            except Exception:
                max_idx = min_idx = None

            if pd.notna(max_idx):
                max_row = group.loc[max_idx]
                val = float(max_row[col])
                val_hr = format_value_for_metric(col, val)
                ax.annotate(
                    val_hr,
                    (max_row["elapsed_min"], max_row[col]),
                    textcoords="offset points", xytext=(0,6), ha="center",
                    fontsize=8, bbox=dict(facecolor="white", alpha=0.75, edgecolor="none")
                )
                summary.append([name, col, "max", val, val_hr, float(max_row["elapsed_min"])])

            if pd.notna(min_idx):
                min_row = group.loc[min_idx]
                val = float(min_row[col])
                val_hr = format_value_for_metric(col, val)
                ax.annotate(
                    val_hr,
                    (min_row["elapsed_min"], min_row[col]),
                    textcoords="offset points", xytext=(0,-10), ha="center",
                    fontsize=8, bbox=dict(facecolor="white", alpha=0.75, edgecolor="none")
                )
                summary.append([name, col, "min", val, val_hr, float(min_row["elapsed_min"])])

    plt.title(title)
    plt.xlabel("Time (minutes since start)")
    plt.ylabel(ylabel)
    if use_formatter:
        ax.yaxis.set_major_formatter(formatter)
    apply_yscale(ax)
    plt.grid(True)
    plt.legend(bbox_to_anchor=(1.05, 1), loc="upper left")
    plt.tight_layout(rect=[0, 0, 0.8, 1])
    plt.savefig(f"{PATH}/{filename}")
    plt.close()

# -------------------------------
# Generate plots
# -------------------------------
plot_stat(data, "cpu_percent", "CPU Usage (%)", "CPU %", "cpu_usage.png")
plot_stat(data, "mem_used_bytes", "Memory Usage", "Memory", "memory_usage.png", use_formatter=True)
plot_stat(data, ["net_in_bytes", "net_out_bytes"], "Network I/O", "Bytes", "network_io.png", use_formatter=True)

# -------------------------------
# Block I/O (split version only) with annotations and summary
# -------------------------------
fig, axes = plt.subplots(1, 2, figsize=(14, 6), sharex=True)
for idx, (name, group) in enumerate(data.groupby("container_name")):
    ls, marker = get_style(idx)
    group = group.sort_values("elapsed_min")
    if "block_in_bytes" in group.columns and group["block_in_bytes"].dropna().any():
        axes[0].plot(group["elapsed_min"], group["block_in_bytes"], ls, marker=marker, markersize=3, label=name)
    if "block_out_bytes" in group.columns and group["block_out_bytes"].dropna().any():
        axes[1].plot(group["elapsed_min"], group["block_out_bytes"], ls, marker=marker, markersize=3, label=name)

    # annotate per column for summary
    for col, ax in zip(["block_in_bytes", "block_out_bytes"], axes):
        if col not in group.columns or group[col].dropna().empty:
            continue
        try:
            max_idx = group[col].idxmax()
            min_idx = group[col].idxmin()
        except Exception:
            max_idx = min_idx = None

        if pd.notna(max_idx):
            max_row = group.loc[max_idx]
            val = float(max_row[col])
            val_hr = format_value_for_metric(col, val)
            ax.annotate(
                val_hr,
                (max_row["elapsed_min"], max_row[col]),
                textcoords="offset points", xytext=(0,6), ha="center",
                fontsize=8, bbox=dict(facecolor="white", alpha=0.75, edgecolor="none")
            )
            summary.append([name, col, "max", val, val_hr, float(max_row["elapsed_min"])])

        if pd.notna(min_idx):
            min_row = group.loc[min_idx]
            val = float(min_row[col])
            val_hr = format_value_for_metric(col, val)
            ax.annotate(
                val_hr,
                (min_row["elapsed_min"], min_row[col]),
                textcoords="offset points", xytext=(0,-10), ha="center",
                fontsize=8, bbox=dict(facecolor="white", alpha=0.75, edgecolor="none")
            )
            summary.append([name, col, "min", val, val_hr, float(min_row["elapsed_min"])])

axes[0].set_title("Block I/O In")
axes[0].set_xlabel("Time (minutes since start)")
axes[0].set_ylabel("Bytes")
axes[0].yaxis.set_major_formatter(formatter)
apply_yscale(axes[0])
axes[0].grid(True)

axes[1].set_title("Block I/O Out")
axes[1].set_xlabel("Time (minutes since start)")
axes[1].yaxis.set_major_formatter(formatter)
apply_yscale(axes[1])
axes[1].grid(True)
axes[1].legend(bbox_to_anchor=(1.05, 1), loc="upper left")

plt.tight_layout(rect=[0, 0, 0.85, 1])
plt.savefig(f"{PATH}/block_io.png")
plt.close()

# -------------------------------
# Save summary CSV (numeric values + human-readable string)
# -------------------------------
summary_df = pd.DataFrame(summary, columns=[
    "container_name", "metric", "type", "value", "value_hr", "at_minute"
])
# ensure correct dtypes
summary_df["value"] = pd.to_numeric(summary_df["value"], errors="coerce")
summary_df["at_minute"] = pd.to_numeric(summary_df["at_minute"], errors="coerce")
summary_df.to_csv(f"{PATH}/summary_stats.csv", index=False)

print(f"✅ Plots saved in ./plots/ with Y-scale = {args.yscale}")
print("📊 Summary saved in ./plots/summary_stats.csv (contains numeric 'value' and human-readable 'value_hr')")
