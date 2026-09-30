---
name: implement-afk
description: "Implement one ticket without user intervention."
---

## Steps

1. Check the current branch and stop in case anything is changed or new and ask the user to resolve it.
2. Check if the current branch is already named after the ticket or spec. If not, switch to develop and create a new branch named after the ticket or spec following the gitflow convention. The name shall also include the ticket or spec number.
3. /wait-subagent `developer,600` Use /tdd where possible, at pre-agreed seams to implement the given ticket. Run typechecking and tests regularly.
4. Commit changes
5. /code-review changes of the last commit save it to docs/reviews 
6. /wait-subagent `reviewer-dev,300` Use /tdd to implement all the findings of code review
7. Commit changes
8. /wait-subagent `static-fixer,300` resolve all the findings of static code analysis
9. Commit changes
10. /wait-subagent `coverage-fixer,300` Use test coverage report and improve tests to achieve coverage numbers listed below
    - 100% for line, method and class
    - +95% for branch and instruction for each class or the current values if that is higher
11. Commit changes
12. /wait-subagent `hardener,900` Use mutation testing report and improve tests to achieve coverage numbers listed below
    - Mutation Coverage +95% and Test Strength +95% or the current values if that is higher 
13. Commit changes
14. /wait-subagent `consolidator,900` Consolidate tests: find duplicates and common bases to reduce redundancy. Make sure to
   - meet all **Acceptance Criteria** specified in the Specification and in the actual Issue.
   - keep the current **Test coverage** for each package
15. Commit changes
