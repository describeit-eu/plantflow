# Code Review: ticket #30 (feature/30-execute-synchronizing-fork-join vs spec/29-synchronizing-fork-join)

## Standards

Hard violations: none.

Judgement calls:
1. Duplicated Code — `DefaultPetriNetEngine.resolveOutputActivationId`: two identical loops over `forkDescriptors`/`joinDescriptors`; extract a helper.
2. Duplicated Code (tests) — `PlantFlowForkJoinSpec`: replacement-token and interleaved-activation tests share ~90% setup; consider one `@Unroll where:` test.
3. Glossary drift — `ForkNetCompiler.groovy:41` variable `mergePlace` holds a Join place on the synchronizing path; rename (GLOSSARY distinguishes Join vs Merge).
4. Feature Envy — `ForkJoinSupport` repeatedly queries `marking` internals; consider a Marking query method.
5. Magic fallback — `ForkJoinSupport.consumeJoinInputTokens`: `if (position < 0) { position = 0 }` may consume another activation's Token; throw or consume nothing instead.

## Spec

(a) Missing/partial:
1. Mixed terminators within one fork (`end merge` + `end fork`) silently accepted by `ForkDiagramBuilder.processForkTerminator`; spec #29 requires rejecting malformed/mismatched terminators.
2. `DiagramValidator.ERR_NO_END_MERGE` message says only "end merge"; misleading for fork/join diagrams.

(b) Scope creep:
- `getJoinDescriptors()` default method added to public `PetriNetStructure` (mirrors `ForkDescriptor`, source-compatible; spec prefers no new public test seam — keep internal if easy).

(c) Suspicious:
1. Latent cross-activation mixing via the `position = 0` fallback in `ForkJoinSupport.consumeJoinInputTokens` (same as Standards #5).
2. Diagram-order deep-merge relies implicitly on place allocation order; make branch order explicit (e.g., ordered inputs in `JoinDescriptor`).

Confirmed correct: exactly-once continuation, replacement-Token correlation, interleaved activations, marking isolation/copy/serialization, `end merge` regression, no timeout.
