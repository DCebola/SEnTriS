import json
import glob
import re
import pandas as pd
import matplotlib.pyplot as plt
import argparse

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

PATH = "plots/lubm"

def add_triples_column(df):
    df["Triples"] = df["Dataset"].map(TRIPLES)
    return df

def parse_report(filepath):
    with open(filepath, "r") as f:
        data = json.load(f)
    summaries = data["aggregate"]["summaries"]

    rows = []
    upload_size = {}
    upload_latency = {}

    # Find upload size
    for k, v in summaries.items():
        if re.match(r"v\d+\.lubm-\d+\.size", k):
            version = k.split(".")[0]
            dataset = k.split(".")[1]
            upload_size[(dataset, version)] = v["mean"]

    # Find upload latencies
    for k, v in summaries.items():
        if "upload-dataset" in k or "upload-ontology" in k:
            m = re.search(r"upload-(?:dataset|ontology)-([^-]+-\d+-v\d+)", k)
            if m:
                dataset_version = m.group(1)
                upload_latency[dataset_version] = v["mean"]

    # Queries
    for k, v in summaries.items():
        if k.startswith("endpoint-metrics.response_time.query"):
            # Example: query-lubm-1-v2-lubm-3
            m = re.search(r"query-(lubm-\d+-v\d+)-lubm-(\d+)", k)
            if not m:
                continue
            dataset_version, query = m.groups()
            latency = v["mean"]

            dataset, version = dataset_version.split("-v")
            version = "v" + version

            # Soundness, completeness, expected, received
            soundness = summaries.get(f"lubm-{query}.soundness", {}).get("mean")
            completeness = summaries.get(f"lubm-{query}.completeness", {}).get("mean")
            expected = summaries.get(f"lubm-{query}.expected", {}).get("mean")
            received = summaries.get(f"lubm-{query}.received", {}).get("mean")

            rows.append({
                "Dataset": dataset,
                "Version": version,
                "Query": f"lubm-{query}",
                "Latency (s)": latency / 1000.0,  # convert ms → seconds
                "Soundness (%)": soundness,
                "Completeness (%)": completeness,
                "Expected": expected,
                "Received": received,
                "Upload Size (GB)": upload_size.get((dataset, version), 0) / (1024**3),
                "Upload Latency (s)": (upload_latency.get(dataset_version, 0)) / 1000.0
            })

    return rows


def process_reports(report_files):
    all_rows = []
    for f in report_files:
        all_rows.extend(parse_report(f))
    df = pd.DataFrame(all_rows)
    return df




if __name__ == "__main__":

    # -------------------------------
    # Parse arguments
    # -------------------------------
    parser = argparse.ArgumentParser(description="Plot lubm benchmark results.")
    parser.add_argument(
        "--experiment",
        type=str,
        default="experiments",
        help="Experiment name.",
    )

    args = parser.parse_args()

    # Load all JSON files in current dir
    files = glob.glob(f"results/{args.experiment}/*.json")
    df = process_reports(files)

    # Add number of triples
    df = add_triples_column(df)

    

    # Save merged results
    df.to_csv(f"{PATH}/merged_results.csv", index=False)
    
    # --- PLOTS ---

    # Latency per dataset-version
    for (dataset, version), group in df.groupby(["Dataset", "Version"]):
        plt.figure(figsize=(10, 6))
        plt.plot(group["Query"], group["Latency (s)"], marker="o", label=f"{dataset}-{version}")
        plt.xticks(rotation=45)
        plt.ylabel("Latency (seconds)")
        plt.title(f"Query Latencies - {dataset}-{version}")
        plt.grid(True, linestyle="--", alpha=0.6)
        plt.legend()
        plt.tight_layout()
        plt.savefig(f"{PATH}/latency_{dataset}_{version}.png")
        plt.close()

    # Combined latency plot
    plt.figure(figsize=(12, 7))
    for (dataset, version), group in df.groupby(["Dataset", "Version"]):
        plt.plot(group["Query"], group["Latency (s)"], marker="o", label=f"{dataset}-{version}")
    plt.xticks(rotation=45)
    plt.ylabel("Latency (seconds)")
    plt.title("Query Latencies (All Datasets & Versions)")
    plt.grid(True, linestyle="--", alpha=0.6)
    plt.legend()
    plt.tight_layout()
    plt.savefig(f"{PATH}/latency_all.png")
    plt.close()

    # Upload size plot
    df.groupby(["Dataset", "Version"]).first()["Upload Size (GB)"].plot(
        kind="bar", figsize=(8, 5), title="Upload Size per Dataset-Version"
    )
    plt.ylabel("Size (GB)")
    plt.grid(True, axis="y", linestyle="--", alpha=0.6)
    plt.tight_layout()
    plt.savefig(f"{PATH}/upload_size.png")
    plt.close()

    # Upload latency plot
    df.groupby(["Dataset", "Version"]).first()["Upload Latency (s)"].plot(
        kind="bar", figsize=(8, 5), title="Upload Latency per Dataset-Version"
    )
    plt.ylabel("Latency (s)")
    plt.grid(True, axis="y", linestyle="--", alpha=0.6)
    plt.tight_layout()
    plt.savefig(f"{PATH}/upload_latency.png")
    plt.close()

    # Soundness & Completeness per query
    for (dataset, version), group in df.groupby(["Dataset", "Version"]):
        if dataset == "lubm-1":
            plt.figure(figsize=(10, 6))
            plt.plot(group["Query"], group["Soundness (%)"], marker="o", label="Soundness")
            plt.plot(group["Query"], group["Completeness (%)"], marker="s", label="Completeness")
            plt.xticks(rotation=45)
            plt.ylabel("Percentage (%)")
            plt.title(f"Soundness & Completeness - {dataset}-{version}")
            plt.grid(True, linestyle="--", alpha=0.6)
            plt.legend()
            plt.tight_layout()
            plt.savefig(f"{PATH}/soundness_completeness_{dataset}_{version}.png")
            plt.close()

# Size vs Triples
    plt.figure(figsize=(8, 5))
    sizes = df.groupby(["Dataset", "Version"]).first()["Upload Size (GB)"]
    triples = df.groupby(["Dataset", "Version"]).first()["Triples"]
    plt.scatter(triples, sizes)
    for (ds, ver), (t, s) in zip(triples.index, zip(triples, sizes)):
        plt.text(t, s, f"{ds}-{ver}", fontsize=8)
    plt.xlabel("Number of Triples")
    plt.ylabel("Upload Size (GB)")
    plt.title("Dataset Size vs Number of Triples")
    plt.grid(True, linestyle="--", alpha=0.6)
    plt.tight_layout()
    plt.savefig(f"{PATH}/size_vs_triples.png")
    plt.close()

    # Upload Latency vs Triples
    plt.figure(figsize=(8, 5))
    upl = df.groupby(["Dataset", "Version"]).first()["Upload Latency (s)"]
    triples = df.groupby(["Dataset", "Version"]).first()["Triples"]
    plt.scatter(triples, upl)
    for (ds, ver), (t, u) in zip(triples.index, zip(triples, upl)):
        plt.text(t, u, f"{ds}-{ver}", fontsize=8)
    plt.xlabel("Number of Triples")
    plt.ylabel("Upload Latency (s)")
    plt.title("Upload Latency vs Number of Triples")
    plt.grid(True, linestyle="--", alpha=0.6)
    plt.tight_layout()
    plt.savefig(f"{PATH}/upload_latency_vs_triples.png")
    plt.close()

    # Query Latency vs Triples
    plt.figure(figsize=(10, 6))
    for (ds, ver), group in df.groupby(["Dataset", "Version"]):
        plt.scatter(group["Triples"], group["Latency (s)"], label=f"{ds}-{ver}")
    plt.xlabel("Number of Triples")
    plt.ylabel("Query Latency (s)")
    plt.title("Query Latency vs Number of Triples")
    plt.grid(True, linestyle="--", alpha=0.6)
    plt.legend()
    plt.tight_layout()
    plt.savefig(f"{PATH}/query_latency_vs_triples.png")
    plt.close()
