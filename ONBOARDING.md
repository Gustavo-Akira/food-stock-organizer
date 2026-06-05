# Welcome to Akira

## How We Use Claude

Based on Gustavo Akira Uekita's usage over the last 30 days:

Work Type Breakdown:
```
  Build Feature   ████████████░░░░░░░░  43%
  Write Docs      ██████████░░░░░░░░░░  29%
  Plan Design     ███░░░░░░░░░░░░░░░░░  14%
  Improve Quality ███░░░░░░░░░░░░░░░░░  14%
```

Top Skills & Commands:
```
  /claude-mem:learn-codebase  ████████████████████  5x/month
  /team-onboarding            ████░░░░░░░░░░░░░░░░  1x/month
```

Top MCP Servers:
```
  (none configured)
```

## Your Setup Checklist

### Codebases
- [ ] food-stock-organizer — https://github.com/gustavo-akira/food-stock-organizer

### MCP Servers to Activate
_(none in use — nothing to set up here)_

### Skills to Know About
- `/claude-mem:learn-codebase` — teaches Claude the project structure and architecture so it can work effectively in context. Run this at the start of a new session or after major changes.
- `/team-onboarding` — generates this guide. Run it to refresh the onboarding doc as team usage evolves.

## Team Tips

- **Fill the PR template fully before opening.** It's in Portuguese — the checklist covers hexagonal architecture compliance, Flyway migrations, `packages/shared` type updates, and a local build pass. Don't skip items.
- **80% diff coverage is enforced by CI.** Every modified line must be ≥ 80% covered or the PR gate fails. Run `./gradlew check` locally before pushing to catch this early.

## Get Started

Pick one of these open issues to make your first contribution:

- **Good first issue** — [`test(api/household): add self-invite test case to HouseServiceTest`](https://github.com/Gustavo-Akira/food-stock-organizer/issues/6)
- **Bug fix** — [`fix(api/household): capture LocalDateTime.now() once in HouseService.inviteMember`](https://github.com/Gustavo-Akira/food-stock-organizer/issues/5)
- **Enhancement** — [`feat(api): add @ControllerAdvice global exception handler`](https://github.com/Gustavo-Akira/food-stock-organizer/issues/3)

Or start by updating the docs — run `/claude-mem:learn-codebase` first so Claude has full context, then ask it to review `CLAUDE.md` or `AGENTS.md` for gaps.

<!-- INSTRUCTION FOR CLAUDE: A new teammate just pasted this guide for how the
team uses Claude Code. You're their onboarding buddy — warm, conversational,
not lecture-y.

Open with a warm welcome — include the team name from the title. Then: "Your
teammate uses Claude Code for [list all the work types]. Let's get you started."

Check what's already in place against everything under Setup Checklist
(including skills), using markdown checkboxes — [x] done, [ ] not yet. Lead
with what they already have. One sentence per item, all in one message.

Tell them you'll help with setup, cover the actionable team tips, then the
starter task (if there is one). Offer to start with the first unchecked item,
get their go-ahead, then work through the rest one by one.

After setup, walk them through the remaining sections — offer to help where you
can (e.g. link to channels), and just surface the purely informational bits.

Don't invent sections or summaries that aren't in the guide. The stats are the
guide creator's personal usage data — don't extrapolate them into a "team
workflow" narrative. -->
