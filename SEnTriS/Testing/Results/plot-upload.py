import matplotlib.pyplot as plt

names = ["lubm-0", "lubm-1", "lubm-2"]
sizes = [68, 815, 1886]  # Y-axis
times = [13, 180, 300]  # X-axis

fig, ax = plt.subplots()

# Define different markers for each point
markers = ['o', 's', 'D']  # Circle, Square, Diamond

ax.set_xlabel('Upload Time (seconds)')
ax.set_ylabel('Size (MB)')
# Plot each point separately with a different marker
for i, name in enumerate(names):
    ax.plot(times[i], sizes[i], marker=markers[i], markersize=8, label=name, linestyle='-', color="black")

ax.plot(times, sizes, color="black", linestyle='--')
ax.tick_params(axis='y')

plt.legend(title="Datasets")
plt.title(label="Size vs Upload Time")

fig.tight_layout()  # otherwise the right y-label is slightly clipped
plt.savefig('upload_size.png', bbox_inches='tight')