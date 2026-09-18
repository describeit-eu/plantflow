---
name: implement
description: "Implement a piece of work based on a spec or set of tickets."
disable-model-invocation: true
---

Implement the work described by the user in the spec or tickets.

1. Check the current branch and stop in case anything is changed or new and ask the user to resolve it.
2. Check if the current branch is already named after the ticket or spec. If not create a new branch named after the ticket or spec following the gitflow convention. The name shall also include the ticket or spec number.
3. Use /tdd where possible, at pre-agreed seams. Run typechecking regularly, single test files regularly, and the full test suite once at the end.
4. Once done, use /code-review to review the work.
5. Commit your work to the current branch.
