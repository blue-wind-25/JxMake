# STATE_NEXT_rdd_log.md — Phase 2 Resolved Design Decisions Log

All resolved design decisions arising during phase 2 implementation.
Each row is tagged with a unique key for grep-based lookup.

**CLI usage:** to read a specific decision, run:
```
grep -Fm1 'RDD_KEY_n' util/CodingStyle.md/formatter/STATE_NEXT_rdd_log.md
```

**Do NOT read this file in full** during a CLI session.
Look up only the specific key(s) referenced in STATE_NEXT.md's RDD index.

---

| Key | Topic | Decision |
|---|---|---|
| RDD_KEY_1 | `JavaModifierPriority` column order for `abstract`/`sealed`/`non-sealed`/`final`/`volatile` | STYLE_JAVA17.md §2's literal table (`abstract`/`sealed`/`non-sealed`/`final` sharing one column, ahead of `volatile`) is not purely additive against the already-COMPLETE `JavaModifierPriority`: the priority map is a single flat structure shared by `DeclarationAlignmentRule` (fields) and `GetterSetterRule` (one-liner methods), with no notion of declaration-kind context, so literally adopting the table moves `final` from column 3 to column 2 and `volatile` from column 2 to column 3 — flipping their relative render order for any group mixing `volatile` and `final` fields. Flagged to the user per the Hard Constraint's "stop and ask before changing existing behavior." User resolved with an authoritative, declaration-kind-specific breakdown: CLASS `[access] static abstract final sealed/non-sealed class`, METHOD `[access] static abstract final method`, FIELD `[access] static final volatile field`. Since `abstract`/`sealed`/`non-sealed` never co-occur with `volatile` (different declaration kinds) and `abstract`/`final`/`sealed`/`non-sealed` are mutually exclusive with each other on any single declaration, a single merged map satisfies all three orderings simultaneously: `public/private/protected=0, static=1, abstract/final/sealed=2, volatile=3`. `non-sealed` has no map entry — it lexes as three tokens (`non` `-` `sealed`), not one, so it can't be a single map key; deferred to whatever future pass needs to special-case it as a unit (no current rule reorders class/interface-level modifiers at all, so this is not yet exercised). Implemented: `JavaModifierPriority.java` map update; `TokenizerCore.java` `sealed`/`permits` added to `KEYWORDS_JAVA` (same "always-keyword, contextual-keyword-as-identifier risk accepted" precedent already set by `var`); new `JavaSpecificRule.enforcePermitsClauseLineBreaking` (STYLE_JAVA17.md §2's `permits` clause inline-vs-wrapped line-breaking, wired into `Formatter.formatOne`'s Java branch right after the Allman-brace pass). Verified via a standalone harness (Config.resolve + Formatter.formatOne, no `Main.java` yet): short `permits` clauses stay inline, long ones wrap with column-aligned permitted types, `non-sealed class` tokenizes and passes through without error, and a wrap-then-reformat round trip is byte-for-byte idempotent. |
