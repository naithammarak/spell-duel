# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status
**Spell Duel** is a 1v1 typing wizard duel against bots: a 2D pixel side-view Java Swing mini project for an OOP course. The student works solo. On 27 Oct 2026 it was re-scoped from a tower-defense game. The old versions stay reachable via tags `td-prototype` (master) and `army-prototype` (proto-army, see `SPEC-ARMY.md`). Do not extend them.
Deadlines: feature freeze **30 Oct** · first submission (PDF + code) **3 Nov** · presentation **4 Nov** (10 min) · final submission **7 Nov 2026 16:00**. The final submission is .jar + source + GitHub link + report PDF on Classroom.
`SPEC.md` is the source of truth for features and day-by-day milestones (D1–D10). `DESIGN.md` is the approved architecture: round-based instant-cast duel rules §5, bots §6, ownership table §9, class diagram. Read both before changing structure. If code must deviate from DESIGN.md, update DESIGN.md first. Where README.md disagrees with SPEC.md, SPEC.md wins (README is updated in D4). `docs/` is gitignored and holds only reference material.

**Migration in progress (D1):** code may still be in package `spelldefender` with tower-defense classes. Target layout:
- Package `spellduel`.
- Keep `typing/` and its tests unchanged except the package name.
- Delete the TD/army model and ui.

## Commands
Plain scripts, no Gradle/Maven. `test.sh` downloads the JUnit 5 console-standalone jar into `lib/` on first run. Compiles with `--release 17` (JDK 17+).
```bash
./test.sh                                                 # compile + run all tests
./test.sh --select-class spellduel.model.GameWorldTest    # one test class
./test.sh --select-method spellduel.model.GameWorldTest#shieldBlocksAttack
./run.sh      # build + play
./build.sh    # create dist/SpellDuel.jar
```
Windows uses `test.bat` / `run.bat` / `build.bat`. CI (`.github/workflows/build.yml`) runs `test.sh` then `build.sh`. When renaming, update the package and jar names in the scripts too.

## Architecture (details in DESIGN.md)
- **Packages**: `spellduel.ui → model → typing`, never the reverse. `model` may use `java.awt` but never `javax.swing`; `ArchitectureTest` enforces this. Everything in `model` except `GameWorld`, `GameStats`, `Assets` is package-private, so `ui` can only talk to `GameWorld`.
- **Thread rule**: every public `GameWorld` method is `synchronized`. Never call Swing while holding the lock; `GameLoop` reads `stats()` then uses `invokeLater`. Pause is a `volatile boolean` in `GameLoop`; never `Thread.suspend()`. Inside `update`, entities only `kill()` themselves; `sweepDead()` removes them at the end of the tick.
- **Time** is game time (`GameWorld.clock`), used for parTime, bot cast bars, shield duration and bot reaction delay. Tests drive it with `update(dt)` and seed every `Random`.
- **Player cast pipeline** lives only in `GameWorld.castTyped()`:
  1. `InputBuffer` returns the text.
  2. `SpellDeck.match` finds the closest of the 3 hand cards (Levenshtein; on a tie the shorter one).
  3. `AccuracyGrader.grade` returns a `Tier`.
  4. FIZZLE keeps the card and the player may retype. Otherwise `deck.consume(card)` redraws that card's word and `release` applies the spell at once. One cast per round.
- **Rounds**: `GameWorld.phase` goes COUNTDOWN → TYPING → REVEAL → next round. Both sides type at once; typing ends when both have cast, `GRACE_TIME` after the bot casts, or at `ROUND_TIME`. Shields last one round.
- **Run**: `GameWindow` owns the endless ladder. HP carries over (`GameStats.nextHp`), a loss restarts at level 0, and `BotProfile.forLevel` builds levels past the table.
- **Spell rules** (rock-paper-scissors, reflect, break shield, cut) live only in `GameWorld.release` and `resolveHit`, decided when the bolt arrives. `interruptCast()` clears the player's `InputBuffer` or drops the bot's `Cast`.
- **Bots** = `BotWizard` + `BotProfile` (a row of `int[][] STATS`) + `BotBrain` (Apprentice / Auror / DarkWizard extends Auror). Brains see the duel through the read-only `DuelView`; Auror and Dark Wizard read `lastPlayerRole`. "What is the player typing" has one definition, `SpellDeck.guessRole`, used by the HUD to highlight a card.
- The 2D arrays `String[][] POOLS` (spell words per role), `int[][] STATS` and `Levenshtein`'s `int[][] dp` exist on purpose for the course's 2D-array topic.
- **Focus**: call `requestFocusInWindow()` after every card switch. `GamePanel` calls `setFocusTraversalKeysEnabled(false)`.

## Rules
- Every course topic (Class/Object, Method, Array, 2D array/ArrayList, Inheritance, Polymorphism, Interface/Abstract, GUI/Event, Graphics, Thread) must be visibly used (map in DESIGN §10). Prefer clear OOP over clever patterns.
- The LLM writes most code, but the student must explain every class at the presentation. Keep code simple, with short Thai comments where intent isn't obvious. After each change, summarize the new or changed classes in plain Thai, so the student can add them to `notes/understanding.md`.
- If time runs short, cut in the order of DESIGN §12. Do not add features after 30 Oct.
- External libraries need a justification (JUnit 5 only so far). Free-licensed assets must be credited in the README and the report.
- `docs/example/` holds past students' reports and example code, as reference only. The report follows its structure: Ch. 1 intro, Ch. 2 class diagram / GUI / event handling / algorithm, Ch. 3 problems / highlights / suggestions.
- The UI and docs are in Thai, with English technical terms.
