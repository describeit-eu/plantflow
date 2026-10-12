---
name: wait-subagent
description: This is a **utility skill** — it is not triggered automatically. It contains the sequence of instructions to spawn a subagent, block until it completes and handle result or timeout. When any other skill's instructions contain the text /wait-subagent, that is a command to load and invoke THIS skill — it is not documentation to read or summarize.
argument-hint: "`<agent-name>,<timeout-seconds>` <task description>"
---

You must spawn a subagent, wait for it to finish and return its result. The subagent runs in isolation; its context does not pollute yours.

Communication to and from subagents should be sparse. Communicate primarily through **context pointers**: to the spec, tickets, notes and previous commits. Don't duplicate information already available via pointers.

## Steps

1. Parse the first argument: split on comma to get `<agent-name>` and `<timeout-seconds>`. Everything after is the `<task description>`.
2. Spawn a subagent named `<agent-name>` with the `<task description>`:
   - **Mistral Vibe**: `tools.agent.spawn({ agentName: "<agent-name>", message: "<task description>" })`
   - **OpenCode / Claude Code**: use the `task` tool with `subagent_type: "<agent-name>"` and `prompt: "<task description>"`. If resuming a previous subagent, pass its `task_id` instead.
   - **Claude CLI**: `claude --bg --name "<descriptive name>" -- "<task description>"`, then poll with `claude agents`.
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
