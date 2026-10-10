---
name: wait-subagent
description: Spawn a subagent with user's message, wait for result, close it, return result.
---

Split the first argument on comma to get name and timeout (format: /spawn-subagent `<name>,<timeout>` <message>). You must execute the following steps in sequence. You must spawn a subagent.

1. `tools.agent.spawn({ agentName: <name>, message: <rest-of-prompt> })`
2. `tools.agent.wait({ agentName: <name>, timeoutSec: <timeout> })`
3. `tools.agent.close({ agentName: <name> })`
4. In case of a timeout, stop execution return the timeout message asking the user to retry otherwise return the received result from the subagent.
