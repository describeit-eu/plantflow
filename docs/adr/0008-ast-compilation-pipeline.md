# AST Compilation Pipeline for Activity Diagrams

Instead of directly translating PlantUML text into Petri Net incidence matrices in a single pass, diagram processing is decoupled into an Abstract Syntax Tree (AST), structural validation, and a compiler using a fluent PetriNetBuilder. This separates syntactic parsing from graph topology generation, prevents combinatorial complexity as diagram grammar expands, and allows independent unit testing of validation rules and net construction.
