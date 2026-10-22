# AgentShield Security Report

**Date:** 2026-10-22T17:14:25.204Z
**Target:** C:\Users\bgmel\ConectaTech\el-tabo\sgs-backend
**Grade:** A (98/100)

## Summary

| Metric | Value |
|--------|-------|
| Files scanned | 2 |
| Total findings | 2 |
| Critical | 0 |
| High | 0 |
| Medium | 2 |
| Low | 0 |
| Info | 0 |
| Auto-fixable | 0 |
| Recognized defenses | 0 |

## Harness Adapters

Matched 2/9 registered adapters.

| Adapter | Confidence | Evidence |
|---------|------------|----------|
| Codex | strong | `AGENTS.md` |
| VS Code | strong | `.vscode`, `.vscode/settings.json` |

## Score Breakdown

| Category | Score |
|----------|-------|
| Secrets | 100/100 |
| Permissions | 90/100 |
| Hooks | 100/100 |
| MCP Servers | 100/100 |
| Agents | 100/100 |

## Findings

### 🔵 No permissions block configured

- **Severity:** medium
- **Category:** permissions
- **File:** `.vscode/settings.json`
- **Description:** settings.json has configuration but no permissions section. Without explicit allow/deny lists, the agent relies on default permissions which may be too broad. Add a permissions block to restrict tool access.
- **Fix:** Add a permissions block with scoped allow and deny lists

### 🔵 No PreToolUse security hooks configured

- **Severity:** medium
- **Category:** misconfiguration
- **File:** `.vscode/settings.json`
- **Description:** No PreToolUse hooks are defined. These hooks can catch dangerous operations before they run, providing an essential security layer.
- **Fix:** Add PreToolUse hooks for security-sensitive operations
