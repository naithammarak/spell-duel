# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status
Spell Defender is a typing-based wizard tower-defense game: a 2D pixel top-down Java Swing mini project for an OOP course. The student works solo. Submission is due **3 Nov 2026** and the 10-minute presentation is on 4 Nov.
**No source code exists yet.** `SPEC.md` is the source of truth for features, the class design, and milestones (M0–M7). Read it before starting any work. Some files that `README.md` links to (`docs/GDD.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, etc.) do not exist yet.

## Commands (planned in README, created in milestone M0)
```bash
./test.sh     # run tests (JUnit 5)
./run.sh      # build + play
./build.sh    # create dist/SpellDefender.jar
```
Windows uses `test.bat` / `run.bat` / `build.bat`. Requires JDK 17+. M0 still has to choose between plain scripts and Gradle (see SPEC.md "Open questions"). Update this section once M0 decides, and add the command for running a single test.

## Architecture (see SPEC.md §3 for the class table)
- **Model and view are separated.** Game logic (`GameWorld`, `Spell`, `Enemy`, `SpellBook`, `Levenshtein`, `AccuracyGrader`, `WaveManager`) must not depend on Swing, so JUnit can test it. Swing (`GameWindow` with CardLayout, `MenuPanel`, `GamePanel`, `ResultPanel`) only draws the model and forwards input to it.
- **Game loop**: a single `GameLoop` thread runs at about 60 FPS and calls `world.update(dt)` then `panel.repaint()`. Movement and cooldowns use `dt`, never the frame count.
- **Thread safety**: the game thread mutates the entity `ArrayList`s while the EDT paints them. Guard access with one lock, or paint from a snapshot. Also defer removals until after the update loop.
- **Pause** uses a `volatile boolean`. Never use `Thread.suspend()/resume()`, even though the example code in `docs/example/javaapplication5` does.
- **Focus**: call `requestFocusInWindow()` on `GamePanel` after every button click or card switch. Otherwise the `KeyListener` stops working.
- **Spell casting pipeline**:
  1. `InputBuffer` collects the typed text and the start time.
  2. `SpellBook.match` finds the closest spell by Levenshtein distance. On a tie it picks the shorter spell.
  3. `AccuracyGrader` returns a `Tier` (PERFECT ×1.25 / Great ×1.0 / Good ×0.6 / Weak ×0.3 / Fizzle ×0). PERFECT requires zero errors within `parTime = length × k + 1s`.
  4. `spell.cast(world, multiplier)`. Each `Spell` holds a `Targeting` strategy (LockOn / Directional / Self).
- `Enemy` subclasses override `takeDamage`. For example, `Dementor` resists every spell except `ExpectoPatronum`.
- `WaveManager` and `TileMap` use `int[][]` tables. This is intentional, so the course's 2D-array topic is covered.

## Rules
- Every course topic (Class/Object, Method, Array, 2D array/ArrayList, Inheritance, Polymorphism, Interface/Abstract, GUI/Event, Graphics, Thread) must be visibly used. Keep the design explainable by a student: prefer clear OOP over clever patterns.
- AI use is allowed, but the student must be able to explain every class. Keep code simple, add short comments where intent isn't obvious, and summarize new classes after each change.
- External libraries are allowed if they are justified. JUnit 5 is the first one. FlatLaf and Gson are optional nice-to-haves.
- Free-licensed pixel assets (itch.io / OpenGameArt) must be credited in the README and the report.
- `docs/example/` holds past students' reports and example code. It is reference material only, not part of the build. The final report follows its structure: Ch. 1 intro, Ch. 2 class diagram / GUI / event handling / algorithm, Ch. 3 problems / highlights / suggestions.
- The UI and docs are in Thai, with English technical terms.
