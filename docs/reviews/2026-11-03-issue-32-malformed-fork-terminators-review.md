# Code Review: ticket #32 (feature/32-reject-malformed-fork-terminators vs spec/29-synchronizing-fork-join)

## Standards
Hard violations:
1. `ActivityDiagramParser.groovy:43` uses `"${ERR_MALFORMED_LINE}"` — groovy-developer SKILL.md:35 mandates `$simple_variable` in GStrings.
2. Spock iron rule — `PumlLineClassifierSpec` lines 48–59 duplicate MALFORMED rows already in the main table (39–45); merge into one table.

Judgement calls:
3. `PumlLineClassifier.isMalformedForkLine` uses magic literal `'fork'`; add `FORK_KEYWORD` constant like siblings.

## Spec
(a) Missing/partial:
1. Case variants (`FORK`, `End Fork`, `END MERGE`, `Fork Again`) classify as IGNORED (case-sensitive matching) and parse silently — must be rejected per AC/story 22; make malformed detection case-insensitive.
2. Blanket `startsWith('end')` false-positives on legitimate `end note`/`end legend` constructs previously tolerated as IGNORED — keep those IGNORED; flag other `end*`/`fork*` near-misses (e.g. `endfork`, `en fork` variants starting with end/fork) as MALFORMED.
3. Optional: report all malformed lines, not just the first.
(b) Keep: if-then-else + fork mix now rejected (justified by spec #29). (c) Cosmetic: mix-check vs malformed precedence.
