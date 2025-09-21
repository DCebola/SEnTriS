#!/usr/bin/env python3
"""
plot-lubm.py

Usage:
  python plot-lubm.py --input-glob "*.json" --output-dir results --log-scale-triples

Outputs:
  - merged_results.csv (raw rows, with human-readable values)
  - latency_<dataset>_<version>.png
  - latency_all.png
  - size_vs_triples.png
  - upload_latency_vs_triples.png
  - query_latency_vs_triples.png
  - soundness_completeness_<dataset>_<version>.png
  - upload_ontology_latency.png
  - upload_dataset_latency.png
"""
import json, glob, re, os, argparse
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.lines import Line2D
import numpy as np

# -----------------------
# Defaults
# -----------------------
DEFAULT_INPUT_GLOB = "results/*.json"
DEFAULT_OUTPUT_DIR = "plots/lubm-final"
# Add this at the top or load from JSON/CSV
TRIPLES = {
    "lubm-0": 8521,
    "lubm-1": 100545,
    "lubm-2": 230053,
    "lubm-3": 337129,
    "lubm-4": 477786,
    "lubm-5": 624534,
    "lubm-10": 1272577,
    "lubm-20": 2688048,
    # extend as needed
}

# -----------------------
# Custom query order
# -----------------------
CUSTOM_QUERY_ORDER = [11, 1, 14, 3, 12, 2, 4, 5, 6, 10, 13, 7, 9, 8] #[13, 10, 3, 1, 11, 14, 6, 7, 4, 5, 12, 9, 2, 8]
CUSTOM_QUERY_MAP = {q: i for i, q in enumerate(CUSTOM_QUERY_ORDER)}


# -----------------------
# Helpers
# -----------------------
def ensure_dir(d): os.makedirs(d, exist_ok=True)

def dataset_sort_key(ds):
    m = re.match(r"lubm-(\d+)", ds)
    return int(m.group(1)) if m else 999999

def version_sort_key(ver):
    m = re.match(r"v(\d+)", ver)
    return int(m.group(1)) if m else 999999

def format_dataset_label(ds, ver, filter_version=None):
    m = re.match(r"lubm-(\d+)", ds)
    n = m.group(1) if m else "?"
    if ver is None or filter_version is not None:
        return f"lubm({n})"
    return f"lubm({n},{ver})"

def format_query_label(qname):
    m = re.match(r"lubm-(\d+)", qname)
    return f"query({m.group(1)})" if m else qname

def sorted_dataset_version_list(df):
    return sorted(
        df.drop_duplicates(subset=["Dataset","Version"])[["Dataset","Version"]].values.tolist(),
        key=lambda x: (dataset_sort_key(x[0]), version_sort_key(x[1]))
    )

# -----------------------
# Parsing
# -----------------------
def parse_report(filepath, triples_map):
    with open(filepath, "r") as fh:
        data = json.load(fh)
    summaries = data.get("aggregate", {}).get("summaries", {})

    upload_size, upload_dataset_latency, upload_ontology_latency = {}, {}, {}
    query_entries = []

    # Upload size
    for k, v in summaries.items():
        m = re.match(r"(v\d+)\.lubm-(\d+)\.size", k)
        if m:
            version, dataset = m.group(1), f"lubm-{m.group(2)}"
            upload_size[(dataset, version)] = float(v.get("mean", 0)) if "mean" in v else None

    # Upload latencies
    for k, v in summaries.items():
        m = re.search(r"endpoint-metrics\.response_time\.upload-(dataset|ontology)-(lubm-\d+-v\d+)", k)
        if m:
            kind, dv = m.groups()
            val = float(v.get("mean", 0)) if "mean" in v else None
            if kind == "dataset":
                upload_dataset_latency[dv] = val
            else:
                upload_ontology_latency[dv] = val

    # Queries
    for k, v in summaries.items():
        if not k.startswith("endpoint-metrics.response_time.query"):
            continue
        m = re.search(r"query-(lubm-\d+-v\d+)-lubm-(\d+)", k)
        if not m:
            continue
        dataset_version, query_num = m.groups()
        latency_ms = float(v.get("mean", 0)) if "mean" in v else None
        m2 = re.match(r"(lubm-\d+)-v(\d+)", dataset_version)
        if not m2:
            continue
        dataset, version = m2.group(1), "v" + m2.group(2)

        query_entries.append({
            "Dataset": dataset,
            "Version": version,
            "Query": f"lubm-{query_num}",
            "QueryNum": int(query_num),
            "Latency (ms)": latency_ms,
            "Latency (s)": latency_ms / 1000 if latency_ms else None,
            "Soundness (%)": summaries.get(f"lubm-{query_num}.soundness", {}).get("mean"),
            "Completeness (%)": summaries.get(f"lubm-{query_num}.completeness", {}).get("mean"),
            "Expected": summaries.get(f"lubm-{query_num}.expected", {}).get("mean"),
            "Received": summaries.get(f"lubm-{query_num}.received", {}).get("mean"),
        })

    rows, src = [], os.path.basename(filepath)

    for q in query_entries:
        ds, ver = q["Dataset"], q["Version"]
        size_bytes = upload_size.get((ds, ver))
        rows.append({
            "SourceFile": src,
            "Dataset": ds,
            "Version": ver,
            "Triples": triples_map.get(ds),
            "Query": format_query_label(q["Query"]),
            "QueryNum": q["QueryNum"],
            "Latency (ms)": q["Latency (ms)"],
            "Latency (s)": q["Latency (s)"],
            "Soundness (%)": q["Soundness (%)"],
            "Completeness (%)": q["Completeness (%)"],
            "Expected": q["Expected"],
            "Received": q["Received"],
            "Upload Size (MB)": (size_bytes / 1024**2) if size_bytes else None,
            "Upload Size (GB)": (size_bytes / 1024**3) if size_bytes else None,
            "Upload Latency (ms)": upload_dataset_latency.get(f"{ds}-{ver}"),
            "Upload Latency (s)": (upload_dataset_latency.get(f"{ds}-{ver}") / 1000)
                                   if upload_dataset_latency.get(f"{ds}-{ver}") else None,
            "Upload Ontology Latency (ms)": upload_ontology_latency.get(f"{ds}-{ver}"),
            "Upload Ontology Latency (s)": (upload_ontology_latency.get(f"{ds}-{ver}") / 1000)
                                           if upload_ontology_latency.get(f"{ds}-{ver}") else None,
        })

    return rows

def process_reports(files, triples_map):
    all_rows = []
    for f in files:
        try: all_rows.extend(parse_report(f, triples_map))
        except Exception as e: print(f"Warning parsing {f}: {e}")
    return pd.DataFrame(all_rows) if all_rows else pd.DataFrame()

# -----------------------
# Styles
# -----------------------
def assign_styles(df):
    dv_list = sorted_dataset_version_list(df)
    markers = ["o","s","D","^","v",">","<","p","X","*","h","8"]
    dv_styles = {(ds,ver): {"marker": markers[i % len(markers)]} for i,(ds,ver) in enumerate(dv_list)}
    line_styles = ["-","--","-.",":"]
    q_linestyles = {q: line_styles[i % len(line_styles)] for i,q in enumerate(sorted(df[df["Query"]!=""]["QueryNum"].unique()))}
    return dv_styles, q_linestyles

def version_color(ver, versions):
    """Return grayscale shade based on version index (darker for lower versions)."""
    idx = versions.index(ver)
    shades = np.linspace(0.2, 0.6, len(versions))  # 0.2=dark, 0.8=light
    return str(shades[idx])

# -----------------------
# Plot helpers (legends)
# -----------------------
def add_query_and_dataset_legends(ax, q_linestyles, dv_styles, filter_version=None):
    """
    Add legends for queries (line styles) and dataset-version markers.

    Parameters:
      ax (matplotlib.Axes) - the plot axis
      q_linestyles (dict) - mapping query -> linestyle
      dv_styles (dict) - mapping (dataset, version) -> style dict
      filter_version (str or None) - if given (e.g., "v1"), only include that version in dataset legend
    """
    if q_linestyles is not None:
        # Queries legend
        q_handles = [Line2D([0],[0], color="black", linestyle=ls, linewidth=2)
                     for q, ls in q_linestyles.items()]
        q_labels = [format_query_label(f"lubm-{q}") for q in q_linestyles.keys()]
        q_leg = ax.legend(q_handles, q_labels, title="Queries (line style)",
                          loc='upper left', fontsize='small')
        ax.add_artist(q_leg)

    # Dataset-version legend
    dv_handles, dv_labels = [], []
    for (ds, ver), s in dv_styles.items():
        if filter_version is not None and ver != filter_version:
            continue
        dv_handles.append(Line2D([0],[0], marker=s["marker"], color="black",
                                 linestyle="None", markersize=8))
        dv_labels.append(format_dataset_label(ds, ver, filter_version))

    if dv_handles:
        if q_linestyles is not None:
            ax.legend(dv_handles, dv_labels, title="Datasets (markers)",
                      bbox_to_anchor=(0.275, 1), loc="upper right", fontsize='small')
        else:
            ax.legend(dv_handles, dv_labels, title="Datasets (markers)",
                      loc='upper left', fontsize='small')

# -----------------------
# Plots
# -----------------------
def plot_latency_per_dataset_version(df, outdir, dv_styles):
    """Plot query latencies per dataset, showing all versions in grayscale, with custom query order."""
    qdf = df[df["Query"] != ""]
    for ds, group_ds in sorted(qdf.groupby("Dataset"), key=lambda x: dataset_sort_key(x[0])):
        fig, ax = plt.subplots(figsize=(10,6))
        versions = sorted(group_ds["Version"].unique(), key=version_sort_key)

        for ver in versions:
            gg = group_ds[group_ds["Version"] == ver]
            # Map QueryNum into custom order index
            gg = gg.assign(Order=gg["QueryNum"].map(lambda q: CUSTOM_QUERY_MAP.get(q, 999)))
            gg = gg.sort_values("Order")

            ax.plot(
                gg["Order"], gg["Latency (s)"],
                marker=dv_styles[(ds, ver)]["marker"],
                color=version_color(ver, versions),
                linestyle="-",
                label=f"{ver}"
            )

        ordered_queries = [q for q in CUSTOM_QUERY_ORDER if q in group_ds["QueryNum"].values]
        ax.set_xticks(range(len(ordered_queries)))
        ax.set_xticklabels([format_query_label(f"lubm-{n}") for n in ordered_queries], rotation=45)
        ax.set_ylabel("Latency (s)")
        ax.set_title(f"Query Latencies - {format_dataset_label(ds, None)}")
        ax.grid(True, linestyle="--", alpha=0.6)
        ax.legend(title="Versions")
        fig.tight_layout()
        fig.savefig(os.path.join(outdir, f"latency_{ds}.png"), dpi=150)
        plt.close(fig)


def plot_combined_latency(df, outdir, dv_styles):
    """Plot query latencies across all datasets, with lines per version in grayscale, with custom query order."""
    qdf = df[df["Query"] != ""]
    fig, ax = plt.subplots(figsize=(12,7))

    for ds, group_ds in sorted(qdf.groupby("Dataset"), key=lambda x: dataset_sort_key(x[0])):
        versions = sorted(group_ds["Version"].unique(), key=version_sort_key)
        for ver in versions:
            gg = group_ds[group_ds["Version"] == ver]
            gg = gg.assign(Order=gg["QueryNum"].map(lambda q: CUSTOM_QUERY_MAP.get(q, 999)))
            gg = gg.sort_values("Order")

            ax.plot(
                gg["Order"], gg["Latency (s)"],
                marker=dv_styles[(ds, ver)]["marker"],
                color=version_color(ver, versions),
                linestyle="-",
                label=f"{format_dataset_label(ds, ver)}"
            )

    ordered_queries = CUSTOM_QUERY_ORDER
    ax.set_xticks(range(len(ordered_queries)))
    ax.set_xticklabels([format_query_label(f"lubm-{n}") for n in ordered_queries], rotation=45)
    ax.set_ylabel("Latency (s)")
    ax.set_title("Query Latencies (All Datasets)")
    ax.grid(True, linestyle="--", alpha=0.6)
    ax.legend(fontsize="small", ncol=2, title="Dataset-Version")
    fig.tight_layout()
    fig.savefig(os.path.join(outdir, "latency_all.png"), dpi=150)
    plt.close(fig)


def plot_query_latency_vs_triples(df, outdir, dv_styles, q_linestyles):
    """One plot per version: query latency vs triples."""
    qdf = df[df["Query"] != ""].dropna(subset=["Triples","Latency (s)"])

    for ver, group_ver in sorted(qdf.groupby("Version"), key=lambda x: version_sort_key(x[0])):
        fig, ax = plt.subplots(figsize=(12,8))

        for qnum, ls in q_linestyles.items():
            rows = group_ver[group_ver["QueryNum"]==qnum].sort_values("Triples")
            if rows.empty: continue
            ax.plot(rows["Triples"], rows["Latency (s)"], linestyle=ls, color="black", alpha=0.8)
            for _, r in rows.iterrows():
                ax.scatter(r["Triples"], r["Latency (s)"],
                           marker=dv_styles[(r["Dataset"], r["Version"])]["marker"],
                           color="black", s=70)

        ax.set_xlabel("Number of Triples")
        ax.set_ylabel("Query Latency (s)")
        ax.set_title(f"Query Latency vs Number of Triples - {ver}")
        ax.grid(True, linestyle="--", alpha=0.6)
        add_query_and_dataset_legends(ax, q_linestyles, dv_styles, ver)
        fig.tight_layout()
        fig.savefig(os.path.join(outdir, f"query_latency_vs_triples_{ver}.png"), dpi=150)
        plt.close(fig)

def plot_size_vs_triples(df, outdir, dv_styles):
    size_df = df.drop_duplicates(subset=["Dataset","Version"]).dropna(subset=["Upload Size (GB)","Triples"])
    size_df = size_df.sort_values(by=["Dataset","Version"], key=lambda col: col.map(lambda v: dataset_sort_key(v) if col.name=="Dataset" else version_sort_key(v)))
    fig, ax = plt.subplots(figsize=(8,6))
    versions = sorted(size_df["Version"].unique(), key=version_sort_key)
    x = dict()
    y = dict()
    for (ds, ver), row in size_df.groupby(["Dataset","Version"]):
        x.setdefault(ver, []).append(row["Triples"])
        y.setdefault(ver, []).append(row["Upload Size (GB)"])
        ax.plot(row["Triples"], row["Upload Size (GB)"], marker=dv_styles[(ds,ver)]["marker"], color=version_color(ver, versions))
    for ver in versions:
        ax.plot(x[ver], y[ver], color=version_color(ver, versions), linestyle="-")
    ax.set_xlabel("Number of Triples"); ax.set_ylabel("Upload Size (GB)")
    ax.set_title("Upload Size vs Number of Triples"); ax.grid(True, linestyle="--", alpha=0.6)
    add_query_and_dataset_legends(ax, None, dv_styles)
    fig.tight_layout(); fig.savefig(os.path.join(outdir, "size_vs_triples.png"), dpi=150); plt.close(fig)

def plot_upload_latency_vs_triples(df, outdir, dv_styles):
    upl = df[df["Upload Latency (s)"].notnull()].drop_duplicates(subset=["Dataset","Version"])
    upl = upl.sort_values(by=["Dataset","Version"],
                          key=lambda col: col.map(lambda v: dataset_sort_key(v) if col.name=="Dataset" else version_sort_key(v)))
    fig, ax = plt.subplots(figsize=(8,6))
    versions = sorted(upl["Version"].unique(), key=version_sort_key)
    x, y = {}, {}
    for (ds, ver), row in upl.groupby(["Dataset", "Version"]):
        x.setdefault(ver, []).append(row["Triples"])
        y.setdefault(ver, []).append(row["Upload Latency (s)"])
        ax.plot(row["Triples"], row["Upload Latency (s)"],
                marker=dv_styles[(ds, ver)]["marker"],
                color=version_color(ver, versions))
    for ver in versions:
        ax.plot(x[ver], y[ver], color=version_color(ver, versions), linestyle="-")
    ax.set_xlabel("Number of Triples")
    ax.set_ylabel("Upload Latency (s)")
    ax.set_title("Upload Latency vs Number of Triples")
    ax.grid(True, linestyle="--", alpha=0.6)
    add_query_and_dataset_legends(ax, None, dv_styles)
    fig.tight_layout()
    fig.savefig(os.path.join(outdir, "upload_latency_vs_triples.png"), dpi=150)
    plt.close(fig)

def plot_soundness_completeness(df, outdir):
    qdf = df[df["Query"] != ""]
    for (ds, ver), group in sorted(qdf.groupby(["Dataset","Version"]), key=lambda x: (dataset_sort_key(x[0][0]), version_sort_key(x[0][1]))):
        gg = group.sort_values("QueryNum")
        if gg["Soundness (%)"].notnull().any() or gg["Completeness (%)"].notnull().any():
            fig, ax = plt.subplots(figsize=(10,6))
            if gg["Soundness (%)"].notnull().any():
                ax.plot(gg["QueryNum"], gg["Soundness (%)"], marker="o", label="Soundness", linestyle="", color="black")
            if gg["Completeness (%)"].notnull().any():
                ax.plot(gg["QueryNum"], gg["Completeness (%)"], linestyle='--', color="black", label="Completeness")
            ax.set_xticks(gg["QueryNum"]); ax.set_xticklabels(gg["Query"], rotation=45)
            ax.set_ylabel("Percentage (%)"); ax.set_ylim(-1,150)
            ax.set_title(f"Soundness & Completeness - {format_dataset_label(ds, ver)}")
            ax.grid(True, linestyle="--", alpha=0.6); ax.legend()
            fig.tight_layout(); fig.savefig(os.path.join(outdir, f"soundness_completeness_{ds}_{ver}.png"), dpi=150); plt.close(fig)

def plot_upload_ontology_latencies(df, outdir):
    ont = df[df["Upload Ontology Latency (s)"].notnull()].drop_duplicates(subset=["Dataset","Version"])
    if ont.empty:
        return

    ont = ont.sort_values(by=["Dataset","Version"],
                          key=lambda col: col.map(lambda v: dataset_sort_key(v) if col.name=="Dataset" else version_sort_key(v)))

    datasets = sorted(ont["Dataset"].unique(), key=dataset_sort_key)
    versions = sorted(ont["Version"].unique(), key=version_sort_key)
    x = np.arange(len(datasets))
    width = 0.8 / len(versions)
    shades = np.linspace(0.3, 0.8, len(versions))  # greyscale fills

    fig, ax = plt.subplots(figsize=(10,6))
    for i, ver in enumerate(versions):
        vals = [ont[(ont["Dataset"]==ds) & (ont["Version"]==ver)]["Upload Ontology Latency (s)"].values[0]
                if not ont[(ont["Dataset"]==ds) & (ont["Version"]==ver)].empty else 0
                for ds in datasets]
        ax.bar(x + i*width, vals, width, label=ver, color=str(shades[i]), edgecolor="black")

    ax.set_xticks(x + width*(len(versions)-1)/2)
    ax.set_xticklabels([format_dataset_label(ds, None) for ds in datasets])
    ax.set_ylabel("Upload Ontology Latency (s)")
    ax.set_title("Upload Ontology Latencies")
    ax.legend(title="Versions")
    ax.grid(True, axis='y', linestyle="--", alpha=0.6)
    fig.tight_layout()
    fig.savefig(os.path.join(outdir, "upload_ontology_latency.png"), dpi=150)
    plt.close(fig)


def plot_upload_dataset_latencies(df, outdir):
    upl = df[df["Upload Latency (s)"].notnull()].drop_duplicates(subset=["Dataset","Version"])
    if upl.empty: return
    upl = upl.sort_values(by=["Dataset","Version"],
                          key=lambda col: col.map(lambda v: dataset_sort_key(v) if col.name=="Dataset" else version_sort_key(v)))

    datasets = sorted(upl["Dataset"].unique(), key=dataset_sort_key)
    versions = sorted(upl["Version"].unique(), key=version_sort_key)
    x = np.arange(len(datasets))
    width = 0.8 / len(versions)
    shades = np.linspace(0.3, 0.8, len(versions))

    fig, ax = plt.subplots(figsize=(10,6))
    for i, ver in enumerate(versions):
        vals = [upl[(upl["Dataset"]==ds) & (upl["Version"]==ver)]["Upload Latency (s)"].values[0]
                if not upl[(upl["Dataset"]==ds) & (upl["Version"]==ver)].empty else 0
                for ds in datasets]
        ax.bar(x + i*width, vals, width, label=ver, color=str(shades[i]), edgecolor="black")

    ax.set_xticks(x + width*(len(versions)-1)/2)
    ax.set_xticklabels([format_dataset_label(ds, None) for ds in datasets])
    ax.set_ylabel("Upload Latency (s)")
    ax.set_title("Upload Dataset Latencies")
    ax.legend(title="Versions")
    ax.grid(True, axis='y', linestyle="--", alpha=0.6)
    fig.tight_layout()
    fig.savefig(os.path.join(outdir, "upload_dataset_latency.png"), dpi=150)
    plt.close(fig)

def plot_upload_dataset_sizes(df, outdir):
    """Plot upload sizes per dataset (grouped bar chart by version, greyscale)."""
    size_df = df[df["Upload Size (GB)"].notnull()].drop_duplicates(subset=["Dataset","Version"])
    if size_df.empty:
        return

    size_df = size_df.sort_values(
        by=["Dataset","Version"],
        key=lambda col: col.map(lambda v: dataset_sort_key(v) if col.name=="Dataset" else version_sort_key(v))
    )

    datasets = sorted(size_df["Dataset"].unique(), key=dataset_sort_key)
    versions = sorted(size_df["Version"].unique(), key=version_sort_key)
    x = np.arange(len(datasets))
    width = 0.8 / len(versions)
    shades = np.linspace(0.3, 0.8, len(versions))  # grayscale fills

    fig, ax = plt.subplots(figsize=(10,6))
    for i, ver in enumerate(versions):
        vals = [size_df[(size_df["Dataset"]==ds) & (size_df["Version"]==ver)]["Upload Size (GB)"].values[0]
                if not size_df[(size_df["Dataset"]==ds) & (size_df["Version"]==ver)].empty else 0
                for ds in datasets]
        ax.bar(x + i*width, vals, width, label=ver, color=str(shades[i]), edgecolor="black")

    ax.set_xticks(x + width*(len(versions)-1)/2)
    ax.set_xticklabels([format_dataset_label(ds, None) for ds in datasets])
    ax.set_ylabel("Upload Size (GB)")
    ax.set_title("Upload Dataset Sizes")
    ax.legend(title="Versions")
    ax.grid(True, axis='y', linestyle="--", alpha=0.6)
    fig.tight_layout()
    fig.savefig(os.path.join(outdir, "upload_dataset_sizes.png"), dpi=150)
    plt.close(fig)



def plot_combined_legend(outdir, dv_styles, q_linestyles):
    fig, ax = plt.subplots(figsize=(8,4))
    ax.axis("off")

    # Dataset-version markers
    dv_handles = [Line2D([0],[0], marker=s["marker"], color="black", linestyle="None", markersize=8)
                  for (ds,ver), s in dv_styles.items()]
    dv_labels = [format_dataset_label(ds, ver) for (ds,ver) in dv_styles.keys()]

    # Query line styles
    q_handles = [Line2D([0],[0], color="black", linestyle=ls, linewidth=2)
                 for q, ls in q_linestyles.items()]
    q_labels = [format_query_label(f"lubm-{q}") for q in q_linestyles.keys()]

    # Combine
    legend1 = ax.legend(dv_handles, dv_labels, title="Datasets (markers)", loc="upper left")
    ax.add_artist(legend1)
    ax.legend(q_handles, q_labels, title="Queries (line styles)", loc="upper right")

    plt.tight_layout()
    plt.savefig(os.path.join(outdir, "legend.png"), dpi=150)
    plt.close()


# -----------------------
# Main
# -----------------------
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-glob", default=DEFAULT_INPUT_GLOB)
    parser.add_argument("--output-dir", default=DEFAULT_OUTPUT_DIR)
    args = parser.parse_args()

    ensure_dir(args.output_dir)
    files = glob.glob(args.input_glob)
    if not files: return print(f"No files matched {args.input_glob}.")
    df = process_reports(files, TRIPLES)
    if df.empty: return print("No data parsed.")

    df.to_csv(os.path.join(args.output_dir, "merged_results.csv"), index=False)
    dv_styles, q_linestyles = assign_styles(df)

    plot_latency_per_dataset_version(df, args.output_dir, dv_styles)
    plot_combined_latency(df, args.output_dir, dv_styles)
    plot_query_latency_vs_triples(df, args.output_dir, dv_styles, q_linestyles)
    plot_size_vs_triples(df, args.output_dir, dv_styles)
    plot_upload_latency_vs_triples(df, args.output_dir, dv_styles)
    plot_soundness_completeness(df, args.output_dir)
    plot_upload_ontology_latencies(df, args.output_dir)
    plot_upload_dataset_latencies(df, args.output_dir)
    plot_upload_dataset_sizes(df, args.output_dir)
    plot_combined_legend(args.output_dir, dv_styles, q_linestyles)

    print("Done. Results in", args.output_dir)

if __name__ == "__main__": main()
