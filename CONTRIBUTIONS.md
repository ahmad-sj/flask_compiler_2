# CONTRIBUTIONS — what the git history actually records

Generated from `git log` on the `main` branch at 61 commits. Every figure below
is reproducible with the command shown beside it. **Nothing here maps a git
identity to a real person** — that mapping is for the team to fill in, in
[MEMBERS.md](MEMBERS.md).

Identities are consolidated by [.mailmap](.mailmap), which merges only pairs the
log itself proves identical (a shared email, or a shared name). It takes the
history from **6 raw identities to 4 canonical ones**:

```
$ git shortlog -sne HEAD        # before .mailmap
    23  ahmad.sj <ahmad-alsarraj@hotmail.com>
    16  Ahmad khaled <eng.ahmadkhaled21@gmail.com>
    12  Dude1o <meladnofal146@gmail.com>
     5  mariaali2000 <mariamariaas2002@gmail.com>
     4  Dude1o <meladnofal91@gmail.com>
     1  ahmad-sj <ahmad-alsarraj@hotmail.com>

$ git shortlog -sne HEAD        # after .mailmap
    24  ahmad.sj <ahmad-alsarraj@hotmail.com>
    16  Ahmad khaled <eng.ahmadkhaled21@gmail.com>
    16  Dude1o <meladnofal146@gmail.com>
     5  mariaali2000 <mariamariaas2002@gmail.com>
```

24 + 16 + 16 + 5 = 61, matching `git rev-list --count HEAD`.

> **One commit is not original team work.** `52723ea` (2026-08-29, *"build:
> regenerate ANTLR parsers from .g4 as build step 1"*) was produced during the
> audit follow-up and committed under `Dude1o <meladnofal146@gmail.com>`. It is
> included in the totals below because it is genuinely in the log, but it
> contributes only **+12 / −12 lines** (one comment line per regenerated file),
> so it does not materially move any figure. Where it could matter — Area 1 — it
> is called out explicitly.

---

## Summary

| Canonical identity | Commits | Share | First commit | Last commit | Lines + | Lines − |
| --- | ---: | ---: | --- | --- | ---: | ---: |
| `ahmad.sj <ahmad-alsarraj@hotmail.com>` | 24 | 39.3% | 2025-11-24 | 2026-06-07 | 70,837 | 38,031 |
| `Ahmad khaled <eng.ahmadkhaled21@gmail.com>` | 16 | 26.2% | 2025-12-20 | 2026-01-03 | 12,824 | 23,235 |
| `Dude1o <meladnofal146@gmail.com>` | 16 | 26.2% | 2026-06-07 | 2026-08-29 | 11,212 | 3,395 |
| `mariaali2000 <mariamariaas2002@gmail.com>` | 5 | 8.2% | 2025-11-27 | 2025-12-10 | 14,788 | 12 |

> **Read the line counts with care.** They include ANTLR-generated sources under
> `src/antlr/` and compiled `.class` files under `bin/`, both of which are
> committed to this repo. A single parser regeneration moves tens of thousands
> of lines without representing tens of thousands of lines of thought. Commit
> counts and the per-area breakdowns further down are the more honest signal;
> for the grammars specifically, see the hand-written `.g4`-only figures in
> Area 1.

**Timeline.** The four identities worked in three barely-overlapping phases:
`mariaali2000` and `Ahmad khaled` in Nov 2025 – Jan 2026, `ahmad.sj` across
Nov 2025 – Jun 2026, and `Dude1o` from Jun 2026 onward. `ahmad.sj`'s last commit
and `Dude1o`'s first are the same day (2026-06-07); the final ~2.5 months of the
project are `Dude1o` alone.

---

## `ahmad.sj <ahmad-alsarraj@hotmail.com>`

*Includes the single commit authored as `ahmad-sj`, same address.*

- **24 commits** (39.3%), **2025-11-24 → 2026-06-07**
- **+70,837 / −38,031** lines

Top-level directories touched (file-touches, not commits):

| Directory | Touches |
| --- | ---: |
| `src` | 596 |
| `grammars` | 59 |
| `tests` | 13 |
| `.idea` | 7 |
| `dependencies` | 1 |
| *(repo root)* | 1 |

Ten most-touched files:

| Touches | File |
| ---: | --- |
| 12 | `src/antlr/templateParserVisitor.java` |
| 12 | `src/antlr/templateParserListener.java` |
| 12 | `src/antlr/templateParserBaseVisitor.java` |
| 12 | `src/antlr/templateParserBaseListener.java` |
| 12 | `src/antlr/templateParser.java` |
| 12 | `src/antlr/templateParser.interp` |
| 11 | `src/app/FlaskCompiler.java` |
| 11 | `grammars/templateParser.g4` |
| 9 | `src/visitors/NodeVisitor.java` |
| 8 | `src/visitors/jinja/JinjaVisitor.java` *(path since removed)* |

**Shape of the work:** the template front end end-to-end — `templateParser.g4`,
`templateLexer.g4`, `templateFragments.g4`, their generated sources, and
`NodeVisitor`. The six generated `templateParser*` files at 12 touches each are
the regeneration footprint of the 11 commits to `templateParser.g4`.

---

## `Ahmad khaled <eng.ahmadkhaled21@gmail.com>`

- **16 commits** (26.2%), **2025-12-20 → 2026-01-03** — the shortest active
  window of the four, about two weeks
- **+12,824 / −23,235** lines (net negative: this identity committed the
  deletion of the `bin/` build artifacts as well as adding them)

Top-level directories touched:

| Directory | Touches |
| --- | ---: |
| `src` | 91 |
| `bin` | 50 |
| `grammars` | 27 |
| `tests` | 2 |
| `.idea` | 2 |

Ten most-touched files:

| Touches | File |
| ---: | --- |
| 11 | `grammars/pythonLexer.g4` |
| 9 | `grammars/pythonParser.g4` |
| 7 | `src/antlr/pythonLexer.java` |
| 7 | `src/antlr/pythonLexer.interp` |
| 6 | `src/antlr/pythonLexer.tokens` |
| 3 | `src/antlr/pythonParserVisitor.java` |
| 3 | `src/antlr/pythonParserListener.java` |
| 3 | `src/antlr/pythonParserBaseVisitor.java` |
| 3 | `src/antlr/pythonParserBaseListener.java` |
| 3 | `src/antlr/pythonParser.java` |

**Shape of the work:** the Python front end — `pythonLexer.g4` and
`pythonParser.g4` are this identity's two most-touched files by a clear margin,
plus their generated output. The 50 `bin/` touches are committed `.class` files.

---

## `Dude1o <meladnofal146@gmail.com>`

*Includes the 4 commits authored as `Dude1o <meladnofal91@gmail.com>`, same name.*

- **16 commits** (26.2%), **2026-06-07 → 2026-08-29**
- **+11,212 / −3,395** lines

Top-level directories touched:

| Directory | Touches |
| --- | ---: |
| `src` | 122 |
| `tests` | 41 |
| *(repo root)* | 28 |
| `project` | 14 |
| `grammars` | 3 |

Ten most-touched files:

| Touches | File |
| ---: | --- |
| 7 | `src/app/FlaskCompiler.java` |
| 4 | `src/visitors/SemanticAnalyzer.java` |
| 4 | `src/app/CodeGenerator.java` |
| 4 | `src/app/AppHandler.java` |
| 4 | `check.ps1` |
| 4 | `REPORT.md` |
| 3 | `src/visitors/AppVisitor.java` |
| 3 | `src/app/TreePrinter.java` |
| 3 | `src/app/TemplatesHandler.java` |
| 3 | `src/app/CompilerConfig.java` |

**Shape of the work:** the back half of the pipeline and the project scaffolding
— pipeline orchestration (`FlaskCompiler`, `AppHandler`, `TemplatesHandler`,
`CompilerConfig`), code generation, semantic analysis, the test harness, the
reports, and the entire demo app. `project/` was created in a single commit,
`c71b478` *"Rework the pipeline to match the static-site-generator spec"*. This
is the only identity with commits in the last 2.5 months of the project.

---

## `mariaali2000 <mariamariaas2002@gmail.com>`

- **5 commits** (8.2%), **2025-11-27 → 2025-12-10**
- **+14,788 / −12** lines — almost entirely additive, consistent with initial
  scaffolding of generated parser sources

Top-level directories touched:

| Directory | Touches |
| --- | ---: |
| `src` | 20 |
| `grammars` | 16 |
| `.idea` | 4 |

All files touched (fewer than ten distinct):

| Touches | File |
| ---: | --- |
| 5 | `grammars/pythonParser.g4` |
| 5 | `grammars/pythonLexer.g4` |
| 4 | `.idea/misc.xml` |
| 2 | `src/antlr/pythonParserVisitor.java` |
| 2 | `src/antlr/pythonParserListener.java` |
| 2 | `src/antlr/pythonParserBaseVisitor.java` |
| 2 | `src/antlr/pythonParserBaseListener.java` |
| 2 | `src/antlr/pythonParser.tokens` |
| 2 | `src/antlr/pythonParser.java` |
| 2 | `src/antlr/pythonParser.interp` |

**Shape of the work:** early Python grammar work, overlapping with
`Ahmad khaled` on the same two `.g4` files. Contributed +270 lines to the
hand-written grammars. No commits after 2025-12-10.

---

## Cross-reference against ROLES.md

[ROLES.md](ROLES.md) assigns five areas to five names: Melad, Aram, Raghad,
Ahmad and Yousef. Below, each area is measured against the files ROLES.md itself
lists for it. **Only three of the five names correspond to any git identity, and
one of those three is ambiguous between two identities** — so for most areas the
question "did the assigned owner do this work?" cannot be answered from the
repository at all. See [MEMBERS.md](MEMBERS.md) notes 1–3.

`Dude1o` is the only identity with a defensible name match (`meladnofal…`
contains `melad`), and even that is a substring match, not proof.

### Area 1 — Grammar, Lexer & Parser (assigned: Melad)

Measured on the **hand-written `grammars/*.g4` only**, excluding generated
`src/antlr/` output, which would otherwise let one regeneration dominate:

| Identity | Commits | Lines + | Lines − | Share of lines added |
| --- | ---: | ---: | ---: | ---: |
| `ahmad.sj` | 21 | 1,778 | 1,343 | **63.0%** |
| `Ahmad khaled` | 9 | 770 | 543 | 27.3% |
| `mariaali2000` | 3 | 270 | 5 | 9.6% |
| `Dude1o` | 2 | 3 | 3 | 0.1% |

Per grammar file, by commits:

| File | Authorship |
| --- | --- |
| `pythonLexer.g4` | `Ahmad khaled` 7, `mariaali2000` 3, `ahmad.sj` 3 |
| `pythonParser.g4` | `Ahmad khaled` 7, `ahmad.sj` 5, `mariaali2000` 3 |
| `templateLexer.g4` | `ahmad.sj` 6, `Dude1o` 2 |
| `templateParser.g4` | `ahmad.sj` 11, `Dude1o` 1 |
| `templateFragments.g4` | `ahmad.sj` 3 |

> ⚠ **If Melad is `Dude1o`, the assigned owner of the grammars wrote 3 of the
> 2,821 added lines in them — 0.1%.** Their 2 commits here are `be1168e` and
> `c71b478`, both pipeline work that incidentally touched a grammar; a third,
> `52723ea`, is the audit-session build fix. The grammars were written by
> `ahmad.sj` (template side) and `Ahmad khaled` with `mariaali2000` (Python
> side). This is the single largest mismatch in the table.

### Area 2 — AST classes & Tree Printer (assigned: Aram)

| Identity | Commits | Lines + | Lines − | Share of lines added |
| --- | ---: | ---: | ---: | ---: |
| `ahmad.sj` | 17 | 3,939 | 1,396 | **73.7%** |
| `Ahmad khaled` | 3 | 742 | 76 | 13.9% |
| `Dude1o` | 6 | 667 | 97 | 12.5% |
| `mariaali2000` | 0 | 0 | 0 | 0% |

> ⚠ **No git identity matches the name "Aram", so the assigned owner cannot be
> located in the history.** The dominant author of `src/models/**` and
> `TreePrinter` is `ahmad.sj`.

### Area 3 — Visitors (assigned: Raghad)

| Identity | Commits | Lines + | Lines − | Share of lines added |
| --- | ---: | ---: | ---: | ---: |
| `ahmad.sj` | 15 | 2,119 | 340 | **84.2%** |
| `Ahmad khaled` | 2 | 334 | 28 | 13.3% |
| `Dude1o` | 3 | 64 | 23 | 2.5% |
| `mariaali2000` | 0 | 0 | 0 | 0% |

> ⚠ **No git identity matches the name "Raghad", so the assigned owner cannot be
> located in the history.** `AppVisitor`, `PythonVisitor`, `TemplateVisitor` and
> `NodeVisitor` are overwhelmingly `ahmad.sj`'s work.

### Area 4 — Symbol Table & Semantic Analysis (assigned: Ahmad)

| Identity | Commits | Lines + | Lines − | Share of lines added |
| --- | ---: | ---: | ---: | ---: |
| `Dude1o` | 9 | 2,599 | 65 | **81.7%** |
| `ahmad.sj` | 11 | 468 | 67 | 14.7% |
| `Ahmad khaled` | 2 | 113 | 55 | 3.6% |
| `mariaali2000` | 0 | 0 | 0 | 0% |

> ⚠ **"Ahmad" is ambiguous between two identities, and neither is the dominant
> author here.** `ahmad.sj` contributed 14.7% and `Ahmad khaled` 3.6% of the
> lines; `Dude1o` wrote 81.7%, including `SemanticAnalyzer`,
> `TemplateSemanticAnalyzer` and the `tests/` fixtures. `ahmad.sj` does lead on
> commit count (11 vs 9), so the two metrics disagree — but on volume of code
> the assigned owner is not the principal author under either reading.

### Area 5 — Code Generation & Demo App (assigned: Yousef)

| Identity | Commits | Lines + | Lines − | Share of lines added |
| --- | ---: | ---: | ---: | ---: |
| `Dude1o` | 4 | 3,371 | 613 | **100%** |
| `ahmad.sj` | 0 | 0 | 0 | 0% |
| `Ahmad khaled` | 0 | 0 | 0 | 0% |
| `mariaali2000` | 0 | 0 | 0 | 0% |

> ⚠ **No git identity matches the name "Yousef", so the assigned owner cannot be
> located in the history.** Every line of `PythonDataExtractor`, `JinjaRenderer`,
> `ExpressionEvaluator`, `CodeGenerator`, `RouteInfo`, `project/**` and
> `tests/runtime-test.js` was authored by `Dude1o`, in 4 commits. This is the
> only area with a single author and no overlap.

### Summary of mismatches

| Area | Assigned owner | Owner locatable in git? | Actual principal author | Owner's share |
| --- | --- | --- | --- | ---: |
| 1 Grammar | Melad | Only via substring match | `ahmad.sj` (63.0%) | **0.1%** |
| 2 AST / Printer | Aram | ❌ **No commits found** | `ahmad.sj` (73.7%) | — |
| 3 Visitors | Raghad | ❌ **No commits found** | `ahmad.sj` (84.2%) | — |
| 4 Symbols / Semantics | Ahmad | ⚠ Ambiguous (2 identities) | `Dude1o` (81.7%) | 14.7% or 3.6% |
| 5 Code generation | Yousef | ❌ **No commits found** | `Dude1o` (100%) | — |

**Stated plainly: in none of the five areas is the ROLES.md owner demonstrably
the principal author of that area.** Three of the five assigned names appear
nowhere in the history. Of the two that do, one holds 0.1% of their area and the
other at most 14.7%. Meanwhile the two identities that actually wrote most of
the compiler — `ahmad.sj` (39.3% of commits, principal author of areas 1, 2 and
3) and `Dude1o` (principal author of areas 4 and 5) — are between them credited
with at most two of the five areas, and `mariaali2000` is not in ROLES.md at all.

**One further fact the team should know before the defense:** ROLES.md was added
in a single commit, `f62b4cc` (2026-08-20), authored by
`Dude1o <meladnofal146@gmail.com>` — the second-to-last commit in the repository.
It is a forward-looking plan for who will *present* each area, written near the
end of the project by one person. It is not, and does not claim to be, a record
of who wrote what. That is a perfectly legitimate thing to have; it is only a
problem if it is presented to examiners as a contribution record, because the
history does not support it.

Nothing above establishes that anyone did or did not work on this project. Pair
programming, one person pushing another's work, shared machines and accounts
that were never configured all produce exactly this pattern. What the history
*does* establish is that the ROLES.md table cannot be corroborated from the
repository, and examiners awarding individual marks will look at `git log`.
Resolve the mapping in [MEMBERS.md](MEMBERS.md) before submitting.

---

## Reproducing every figure

```sh
# identities, before and after .mailmap
git shortlog -sne HEAD

# commits, dates, per identity (mailmap-aware: %aE, not %ae)
git log --format='%aE|%ad' --date=short HEAD

# lines added/removed for one identity
git log --format='C %aE' --numstat HEAD \
  | awk '/^C /{c=$2;next} NF==3 && $1!="-" && c=="EMAIL"{a+=$1;d+=$2} END{print a, d}'

# directories and most-touched files for one identity
git log --format='%aE|%H' HEAD | awk -F'|' '$1=="EMAIL"{print $2}' > /tmp/h
git log --no-walk --name-only --format='' $(cat /tmp/h) | grep -v '^$' \
  | awk -F/ '{print (NF==1 ? "(root)" : $1)}' | sort | uniq -c | sort -rn

# authorship of one area
git log --format='%aE' HEAD -- <paths> | sort | uniq -c | sort -rn
```
