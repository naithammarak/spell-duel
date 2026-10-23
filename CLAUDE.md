# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status
Spell Defender is a typing-based wizard tower-defense game: a 2D pixel top-down Java Swing mini project for an OOP course. The student works solo. Submission is due **3 Nov 2026** and the 10-minute presentation is on 4 Nov.
`SPEC.md` is the source of truth for features and milestones. `DESIGN.md` is the approved architecture (packages, thread rule, ownership table, class diagram). Read both before changing structure; if code must deviate from DESIGN.md, update DESIGN.md first. Where README.md disagrees with SPEC.md, SPEC.md wins. `docs/` is gitignored (it holds only reference material).

## Commands
Plain scripts, no Gradle/Maven. `test.sh` downloads the JUnit 5 console-standalone jar into `lib/` on first run. Compiles with `--release 17` (JDK 17+).
```bash
./test.sh                                                     # compile + run all tests
./test.sh --select-class spelldefender.model.GameWorldTest    # one test class
./test.sh --select-method spelldefender.model.GameWorldTest#protegoShieldAbsorbsDamage
./run.sh      # build + play
./build.sh    # create dist/SpellDefender.jar
```
Windows uses `test.bat` / `run.bat` / `build.bat`. CI (`.github/workflows/build.yml`) runs `test.sh` then `build.sh`.

## Architecture (details in DESIGN.md)
- **Packages**: `spelldefender.ui → model → typing`, never the reverse. `model` may use `java.awt` (entities draw themselves with `Graphics2D`) but never `javax.swing`; `ArchitectureTest` enforces this. Everything in `model` except `GameWorld`, `GameStats`, `Assets` is package-private, so `ui` can only talk to `GameWorld`.
- **Thread rule**: every public `GameWorld` method is `synchronized`. Never call Swing while holding it; `GameLoop` reads `stats()` then uses `invokeLater`.
- **Time** is game time (`GameWorld.clock` = sum of dt), used for cooldowns and typing parTime, so pause doesn't count and tests drive time with `update(dt)`.
- **Tests** live in the same package as the model and build worlds with the package-private constructor `new GameWorld(SpellBook.standard(), WaveManager.idle(), TileMap.standard())` plus `addEnemy(...)`.
- **Balance numbers** (HP, speed, damage, cooldown, wave table) sit as constructor arguments in each `Enemy`/`Spell` subclass and in `WaveManager.STANDARD_WAVES`. Last tuned with a bot: typing ~4 chars/s usually wins.
- **Game loop**: a single `GameLoop` thread (~60 FPS) calls `world.update(dt)` then `panel.repaint()`. Inside `update`, entities only `kill()` themselves; `sweepDead()` removes them once at the end of the tick.
- **Pause** uses a `volatile boolean` in `GameLoop`. Never use `Thread.suspend()/resume()`, even though the example code in `docs/example/javaapplication5` does.
- **Focus**: call `requestFocusInWindow()` after every card switch or dialog (`GameWindow.show`/`focusCurrent`). `GamePanel` calls `setFocusTraversalKeysEnabled(false)` so Tab reaches the KeyListener.
- **Spell casting pipeline** lives entirely in `GameWorld.castTyped()`:
  1. `InputBuffer` gives the typed text and the elapsed game time since the first key.
  2. `SpellBook.match` returns the closest spell by Levenshtein distance; on a tie it picks the shorter spell.
  3. `AccuracyGrader.grade` returns a `Tier` (PERFECT ×1.25 / Great ×1.0 / Good ×0.6 / Weak ×0.3 / Fizzle ×0). PERFECT requires zero errors within `parTime = length × k + 1s`.
  4. `spell.cast(world, tier)` is a final template method: it checks cooldown, then delegates to the spell's `Targeting` (LockOn / Directional / Self). It returns false when there is no target, which counts as Fizzle. On hit, `GameWorld` calls the subclass's `impact()`.
- **Lock-on target** has one definition, `GameWorld.currentLockTarget()`. The marker drawing and casting both use it.
- **The Dementor rule** lives only in `Dementor.takeDamage`. Spells just report `damageType()`.
- `WaveManager` (`int[][] waveTable`), `TileMap` (`int[][] tiles`, and the base and spawn points derived from it) and `Levenshtein` (`int[][] dp`) cover the 2D-array topic on purpose.

## Rules
- Every course topic (Class/Object, Method, Array, 2D array/ArrayList, Inheritance, Polymorphism, Interface/Abstract, GUI/Event, Graphics, Thread) must be visibly used. Keep the design explainable by a student: prefer clear OOP over clever patterns.
- AI use is allowed, but the student must be able to explain every class. Keep code simple, add short comments where intent isn't obvious, and summarize new classes after each change.
- External libraries are allowed if they are justified. JUnit 5 is the first one. FlatLaf and Gson are optional nice-to-haves.
- Free-licensed pixel assets (itch.io / OpenGameArt) must be credited in the README and the report.
- `docs/example/` holds past students' reports and example code. It is reference material only, not part of the build. The final report follows its structure: Ch. 1 intro, Ch. 2 class diagram / GUI / event handling / algorithm, Ch. 3 problems / highlights / suggestions.
- The UI and docs are in Thai, with English technical terms.
