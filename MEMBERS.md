# Group Members

> **TEMPLATE — Full Name and Student ID are deliberately blank. Fill them in.**
>
> The *Git identity used* column is pre-filled **only** where the git log itself
> proves the link. Nothing in this file is inferred from knowing who anyone is.
> Evidence and caveats for every pre-filled cell are in the notes below, and the
> full breakdown is in [CONTRIBUTIONS.md](CONTRIBUTIONS.md).

| Full Name | Student ID | Git identity used | Area owned |
| --- | --- | --- | --- |
| | | `Dude1o <meladnofal146@gmail.com>` *(see note 1)* | 1 — Grammar, Lexer & Parser |
| | | **NO COMMITS FOUND** *(see note 2)* | 2 — AST classes & Tree Printer |
| | | **NO COMMITS FOUND** *(see note 2)* | 3 — Visitors |
| | | **AMBIGUOUS** *(see note 3)* | 4 — Symbol Table & Semantic Analysis |
| | | **NO COMMITS FOUND** *(see note 2)* | 5 — Code Generation & Demo App |
| | | `ahmad.sj <ahmad-alsarraj@hotmail.com>` | *(see note 4 — no area assigned in ROLES.md)* |
| | | `Ahmad khaled <eng.ahmadkhaled21@gmail.com>` | *(see note 4 — no area assigned in ROLES.md)* |
| | | `mariaali2000 <mariamariaas2002@gmail.com>` | *(see note 5 — not listed in ROLES.md)* |

The area numbers and names come from [ROLES.md](ROLES.md). The last three rows
exist because the history contains contributors that ROLES.md does not account
for; add or remove rows as the real membership requires.

---

## Notes on every pre-filled cell

**1 — `Dude1o` ↔ area 1 is a string match, not a verified identity.**
ROLES.md names *Melad* as the owner of area 1, and the address
`meladnofal146@gmail.com` contains the substring `melad`. That is the only
evidence, and it comes from the log rather than from outside knowledge. It is
**not** proof: confirm it before submitting. Note also that this identity's
actual commit record is concentrated in areas 4 and 5, not area 1 — see the
cross-reference in [CONTRIBUTIONS.md](CONTRIBUTIONS.md) and the finding below.

**2 — *Aram*, *Raghad* and *Yousef* match no git identity at all.**
Searched case-insensitively against every author *and* committer name and email
in the history. Zero hits for each:

```
$ git log --format='%aN <%aE>' HEAD | sort -u | grep -i -e aram -e raghad -e yousef
(no output)
```

The four canonical identities in the log are `ahmad.sj`, `Ahmad khaled`,
`Dude1o` and `mariaali2000`. If these three people committed, they did so under
one of those identities, and the mapping must be supplied by the team — it
cannot be recovered from the repository.

**3 — *Ahmad* is ambiguous between two distinct identities.**
Two identities match the string `ahmad`, with different addresses and different
handles:

- `ahmad.sj <ahmad-alsarraj@hotmail.com>` — 24 commits
- `Ahmad khaled <eng.ahmadkhaled21@gmail.com>` — 16 commits

[.mailmap](.mailmap) deliberately does **not** merge them: a shared first name
is not evidence of a shared person. Whoever *Ahmad* in ROLES.md refers to, pick
the correct one here and give the other its own row.

**4 — Two identities carry substantial work but own no ROLES.md area.**
`ahmad.sj` (24 commits, the largest single share) and `Ahmad khaled` (16
commits) are both major contributors. Only one ROLES.md name — *Ahmad* — could
correspond to either, so at least one of these two people is unaccounted for in
the role table.

**5 — `mariaali2000` appears nowhere in ROLES.md.**
5 commits between 2025-11-27 and 2025-12-10, concentrated in
`grammars/pythonLexer.g4` and `grammars/pythonParser.g4` (+270 lines to the
hand-written grammars). This is a real contributor with no assigned area.

---

## Verifying this file

```sh
git shortlog -sne HEAD          # the four canonical identities
git log --format='%aN <%aE>' HEAD | sort -u
```

Once the Full Name column is filled in, every name should map to exactly one of
the four identities, and every identity should be claimed by exactly one person.
