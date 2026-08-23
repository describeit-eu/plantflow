# Explicit Intermediate Branch Places for Conditionals

PlantUML `if (...) then (...) else (...) endif` constructs are translated into Petri Nets using explicit intermediate branch places. The decision place connects to pure branching transitions (`T_yes` and `T_no`) carrying the respective guard predicates; each branching transition outputs to a dedicated branch place (`P_then`, `P_else`), which in turn feeds the branch action transitions before merging at `P_endif`. This cleanly isolates routing logic from action execution.
