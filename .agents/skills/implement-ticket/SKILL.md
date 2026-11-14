---
name: implement-ticket
description: "Implement one ticket without user intervention."
---

Communication to and from sub-agents should be sparse. Communicate primarily through **context pointers**: to the spec, tickets, research notes, and previous commits. Don't duplicate information already available via pointers.

Follow these instructions because it is designed to control the context window.
- You must not deviate from the `Sequence of Steps` described bellow. 
- Follow the instructions of each step because it is designed to control the context of the current agent and its sub-agent.
- For the request 'Use a sub-agent' choose the agent type you think is best suited for the task and start it with a fresh context window.

## Sequence of Steps

1. Check the current branch and stop in case anything is changed or new and ask the user to resolve it.
2. Check if the current branch is named after the ticket being implemented. Stop in case of a wrong branch and ask the user to switch to the correct branch.
3. Use a sub-agent to implement the given ticket. Use /tdd at pre-agreed seams. Run typechecking and tests regularly and commit changes when finished. 
4. /code-review changes of the last commit save it to docs/reviews
5. Use a sub-agent to implement all the findings of code-review. Use /tdd and commit changes when finished.
6. Use a sub-agent to resolve all the findings of static code analysis and commit changes when finished.
7. Use a sub-agent to check the test coverage report and improve tests to achieve coverage numbers listed below and commit changes when finished.
   - 100% for line, method and class
   - +95% for branch and instruction for each class or the current values if that is higher
8. Use a sub-agent to check the mutation testing report and improve tests to achieve coverage numbers listed below and commit changes when finished.
   - Mutation Coverage +95% and Test Strength +95% or the current values if that is higher 
9. Use a sub-agent to consolidate tests: find duplicates and common bases to reduce redundancy and commit changes when finished. Make sure to
   - meet all **Acceptance Criteria** specified in the Specification and in the actual Issue.
   - keep the current **Test coverage** for each package
   - do **NOT** execute mutation testing during test consolidation
