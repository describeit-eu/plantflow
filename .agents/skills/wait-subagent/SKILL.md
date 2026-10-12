---
name: wait-subagent
description: Utility skill to run a subagent, wait to completion and return its result. Invoke explicitly with `<agent-name>,<timeout-seconds>` followed by the task description. Not triggered automatically — call it by name when you need to dispatch a subagent.
argument-hint: "<agent-name>,<timeout-seconds> <task description>"
---

Dispatch a subagent, wait for it to finish, and return its result. The subagent runs in isolation; its context does not pollute yours.

This is a **utility skill** — it is not triggered automatically. Invoke it explicitly by name (`/wait-subagent`) when you or another skill needs to run a subagent to completion.

## Steps

1. Parse the first argument: split on comma to get `<agent-name>` and `<timeout-seconds>`. Everything after is the task message.
2. Spawn a subagent named `<agent-name>` with the task message:
   - **Mistral Vibe**: `tools.agent.spawn({ agentName: "<agent-name>", message: "<task message>" })`
   - **OpenCode / Claude Code**: use the `task` tool with `subagent_type: "<agent-name>"` and `prompt: "<task message>"`. If resuming a previous subagent, pass its `task_id` instead.
   - **Claude CLI**: `claude --bg --name "<descriptive name>" -- "<task message>"`, then poll with `claude agents`.
   - **Other platforms**: use the platform's subagent or background-agent mechanism. If none exists, tell the user the platform does not support subagents and stop.
3. Wait for the subagent to complete, up to `<timeout-seconds>`:
   - **Mistral Vibe**: `tools.agent.wait({ agentName: "<agent-name>", timeoutSec: <timeout> })`
   - **OpenCode / Claude Code**: the `task` tool blocks until completion and returns the result directly.
   - **Claude CLI**: poll `claude agents` until the job shows complete, then read its output.
   - **Other platforms**: use the platform's wait/poll/status mechanism to block or poll until the subagent finishes or `<timeout-seconds>` elapse. If the platform has no way to wait for a background agent, poll its status command on an interval (e.g. every 5–10 seconds) until completion or timeout.
4. **Completion criterion**: the subagent has returned a result or the timeout has elapsed.
5. Close the subagent:
   - **Mistral Vibe**: `tools.agent.close({ agentName: "<agent-name>" })`
   - **OpenCode / Claude Code**: automatic, no close step needed.
   - **Claude CLI**: the background job ends on its own.
   - **Other platforms**: use the platform's cleanup/close/terminate mechanism if one exists (e.g. kill the background process, delete the agent session). If the platform cleans up automatically or has no cleanup step, note that and move on.
6. **On timeout**: stop and report the timeout to the user, asking them to retry or increase the timeout. Do not return a partial result as if it were complete.
7. **On success**: return the subagent's result to the user. If the subagent produced files (reports, notes, diffs), reference them by path.
