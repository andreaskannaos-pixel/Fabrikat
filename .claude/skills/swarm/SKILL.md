---
name: swarm
description: Fan a large task out to many parallel subagents and merge the results. Use ONLY when the user explicitly asks for a swarm, parallel agents, or "N agents" - never on your own judgment, it multiplies token cost by the agent count.
argument-hint: <task> [max agents]
---

# Swarm

Orchestrator pattern: split → fan out → verify → merge. You are the orchestrator; you never delegate the split or the merge.

## 1. Split

- List units of work that are independent: no unit needs another's output, no two writers touch the same lines.
- Classify each: **read-only** (research, review, bug hunt, proposal) or **writer** (edits files).
- Caps unless the user gives a number: 12 read-only, 4 writers. In this repo `index.html` is one file: writers must own disjoint, named regions (functions/sections), max 4.
- If the work does not split cleanly, say so and do it single-agent. Do not pad the count.
- Tell the user the agent count and rough cost multiplier before spawning.

## 2. Fan out

Spawn all agents in one message, in the background. Writers get `isolation: "worktree"`; read-only agents do not.

Each brief is self-contained (agents start cold):

```
Goal: <one unit, one sentence>
Scope: <files / functions / line ranges owned; everything else is read-only>
Context: <the 3-10 facts needed; paths, conventions, how to run/check>
Done when: <observable check>
Return: <exact shape: findings as file:line list | summary of diff + check output>
Do not: touch files outside scope, spawn agents, commit to other branches, push.
```

## 3. Collect

- Wait for completion notifications; never invent or predict a result.
- A report is a claim, not a fact. Read-only: spot-check the top findings against the code. Writers: read the diff.
- Failed or off-scope agent: discard its output, redo that unit yourself or respawn once with a corrected brief.

## 4. Merge

- Writers: merge one worktree at a time into the working branch; after each, run the project check (here: `browser-check` - page loads, console clean, the changed feature works). Resolve conflicts yourself.
- Read-only: dedupe, rank, and give one synthesized result - not N reports.
- Report: units done, units failed, what was verified and how, what was not.
