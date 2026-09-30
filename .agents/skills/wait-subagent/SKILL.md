---
name: wait-subagent
description: Spawn a subagent with user's message, wait for result, close it, return result.
disable-model-invocation: true
---

Split the first argument on comma to get name and timeout (format: /spawn-subagent `<name>,<timeout>` <message>, then execute the following steps in sequence:

1. `tools.agent.spawn({ agentName: <name>, message: <rest-of-prompt> })`
2. `tools.agent.wait({ agentName: <name>, timeoutSec: <timeout> })`
3. `tools.agent.close({ agentName: <name> })`
4. Return the waited result
