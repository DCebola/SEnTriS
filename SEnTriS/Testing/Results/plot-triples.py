import matplotlib.pyplot as plt

names = ["lubm-0", "lubm-1", "lubm-2"]
triples = [8521, 100545, 230053, 337129, 477786, 624534, 1272577, 2688048]
times = [13, 180, 300]  # X-axis

fig, ax = plt.subplots()

# Define different markers for each point
markers = ["o", "s", "D", "H", "X"]

ax.set_xlabel('Upload Time (seconds)')
ax.set_ylabel('Triples')
# Plot each point separately with a different marker
for i, name in enumerate(names):
    ax.plot(times[i], triples[i], marker=markers[i], markersize=8, label=name, linestyle='-', color="black")


ax.plot(times, triples[:len(names)], color="black", linestyle='--')
ax.tick_params(axis='y')

plt.legend(title="Datasets")
plt.title(label="Triple Count vs Upload Time")

fig.tight_layout()  # otherwise the right y-label is slightly clipped
plt.savefig('upload_triples.png', bbox_inches='tight')
