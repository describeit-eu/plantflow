---
name: implement
description: "Implement a piece of work based on a spec or set of tickets."
disable-model-invocation: true
---

Implement the work described by the user in the given spec or tickets.

1. Check the current branch and stop in case anything is changed or new and ask the user to resolve it.
2. Check if the current branch is already named after the ticket or spec. If not, switch to develop and create a new branch named after the ticket or spec following the gitflow convention. The name shall also include the ticket or spec number.
3. Use /tdd where possible, at pre-agreed seams. Run typechecking and tests regularly.
