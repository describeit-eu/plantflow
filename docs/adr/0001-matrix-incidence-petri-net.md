# Matrix Incidence Model for Petri Net

We use an algebraic matrix incidence model to represent places, transitions, and arc connections rather than a direct node-edge object graph. This separates the topological structure of the net from dynamic token markings and handler evaluation, allowing matrix-based marking transformations while supporting coloured token extensions.
