# Cascadia

A digital adaptation of the award-winning board game **Cascadia**, developed in Kotlin using the Board Game Workstation (BGW) framework. This project was created as part of the Software-Projekt (SoPra) at TU Dortmund University.

---

## Table of Contents

- [Overview](#overview)
- [Game Description](#game-description)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Requirements](#requirements)
- [Installation and Execution](#installation-and-execution)
  - [Prerequisites](#prerequisites)
  - [Running the Game](#running-the-game)
  - [Executing Tests](#executing-tests)
- [How to Play](#how-to-play)
  - [Game Objective](#game-objective)
  - [Turn Overview](#turn-overview)
  - [Scoring](#scoring)
- [Controls and Navigation](#controls-and-navigation)
- [Project Structure](#project-structure)
- [Technologies and Dependencies](#technologies-and-dependencies)

---

## Overview

Cascadia is a puzzly tile-laying and token-drafting board game set in the Pacific Northwest. Players take turns building out their terrain and populating it with wildlife to create diverse, harmonious ecosystems.

This application provides a complete graphical desktop interface with local hotseat multiplayer, intelligent bot opponents, network multiplayer capabilities, and state serialization for saving and loading games.

---

## Game Description

In Cascadia, players construct a habitat map composed of hexagonal tiles featuring five distinct habitat types:
- Mountains
- Rivers
- Forests
- Wetlands
- Prairies

Each tile can host specific types of wildlife:
- Bears
- Elk
- Salmon
- Hawks
- Foxes

On each turn, a player selects a combination of one habitat tile and one wildlife token from the market (display of four pairs), places the tile on their board adjacent to existing tiles, and optionally places the wildlife token on a compatible empty habitat tile.

---

## Key Features

- **Local Hotseat Multiplayer:** Play locally with 2 to 4 human players or AI bots on a single screen.
- **AI Opponents:** Configurable bot difficulties:
  - **Random Bot:** Makes valid randomized choices for casual play and simulation testing.
  - **Strong Bot:** Evaluates potential board positions and scoring cards using strategic heuristics.
  - **Configurable Simulation Speed:** Adjust bot execution speed (Slow, Medium, Fast) for smooth observation.
- **Online Multiplayer:** Full networking support over the Network Transfer Format (NTF) and BGW-Net protocol. Host lobbies or join existing game sessions across the network.
- **Save and Load System:** Complete game state serialization, allowing matches to be saved at any turn and resumed seamlessly later.
- **Interactive Hexagonal Board GUI:**
  - Dynamic camera controls (pan, zoom, reset view) to comfortably navigate large habitat maps.
  - Tile rotation prior to placement to optimize habitat adjacencies.
  - Visual validation of valid tile placements and wildlife compatibility.
- **Rule Enforcement:**
  - Automated overpopulation detection and handling (mandatory wipe on 4 identical tokens, optional wipe on 3 identical tokens).
  - Nature Token (Pinecone) mechanics: spend tokens to pick any combination of tile and wildlife, or wipe chosen tokens from the market.
  - Keystone tiles award nature tokens upon placing the designated wildlife.
- **End-Game Scoring Engine:**
  - Fully automated scoring according to active wildlife scoring cards.
  - Largest contiguous habitat territory scoring with player majority bonuses.
  - Nature tokens bonus points.
  - Detailed final score summary view.

---

## Architecture

The project adheres to a clean layered architecture with a clear separation of concerns:

- **Entity Layer (`entity`):** Pure data models representing the game domain (`CascadiaGame`, `Player`, `HabitatTile`, `WildlifeToken`, `Habitat`, `Wildlife`, `ScoringCard`, `GameState`, etc.).
- **Service Layer (`service`):** Contains business logic and state mutators:
  - `RootService`: Central hub coordinating all specific services.
  - `GameActionService`: Manages game setup, rounds, turns, shop restocking, overpopulation checks, save/load persistence, and final scoring calculations.
  - `PlayerActionService`: Handles player interactions (drafting pairs, rotating/placing tiles, placing wildlife tokens, spending nature tokens).
  - `BotActionService` and `BotDecisionService`: Implements AI strategy and decision making.
  - `NetworkService`: Integrates with the BGW network client to serialize and synchronize actions over the network.
- **GUI Layer (`gui`):** Built on the Board Game Workstation (BGW) library:
  - `CascadiaApplication`: Main entry point handling scene switching and global lifecycle.
  - `MainMenuScene`: Landing menu for game mode selection.
  - `LocalLobbyScene` / `OnlineLobbyScene` / `JoinGameScene`: Setup lobbies for local and remote play.
  - `GameScene`: Main play area with interactive hexagonal grid, draft market, player overview, and board navigation.
  - `RulesScene`: In-game rule reference and scoring guide.
  - `ResultMenuScene`: Detailed breakdown of final player scores.

---

## Requirements

- **Java Development Kit (JDK):** Version 11 or higher (configured with Gradle Toolchains for Java 11).
- **Gradle:** Wrapper included (`gradlew` / `gradlew.bat`).
- **Operating System:** Windows, macOS, or Linux with graphical desktop environment support.

---

## Installation and Execution

### Prerequisites

Ensure you have a working Java 11+ runtime installed. The project uses the Gradle wrapper to manage all Kotlin and library dependencies automatically.

### Running the Game

To build and run the application from the project root:

On Linux and macOS:
```bash
./gradlew run
```

On Windows (Command Prompt / PowerShell):
```cmd
gradlew.bat run
```

### Executing Tests

To run the automated unit and integration tests:

On Linux and macOS:
```bash
./gradlew test
```

On Windows:
```cmd
gradlew.bat run
```

To run complete verification including test coverage reports:
```bash
./gradlew check
```

---

## How to Play

### Game Objective

Build the most harmonious ecosystem by placing habitat tiles and wildlife tokens to maximize points according to wildlife scoring cards and contiguous habitat regions.

### Turn Overview

1. **Check Overpopulation:** If the market contains 4 identical wildlife tokens, they are automatically wiped and replaced. If there are 3 identical tokens, the active player may choose to wipe and replace them once per turn.
2. **Drafting:** Select one habitat tile and one wildlife token from the market.
   - Standard action: Choose one of the 4 matched pairs.
   - Nature Token action: Spend a nature token to pick any tile and any wildlife token from different slots, or wipe any number of wildlife tokens from the market before drafting.
3. **Placing the Habitat Tile:** Place the selected tile adjacent to at least one already placed tile on your board. The tile can be rotated into any of the six hexagonal orientations before placement.
4. **Placing the Wildlife Token:** Place the wildlife token onto an empty habitat tile that supports that animal species. If placement is not desired or impossible, the token may be discarded.
5. **Keystone Reward:** If a wildlife token is placed onto a matching keystone tile, earn one Nature Token.
6. **Restock:** The market is replenished with a new tile and token from the draw piles.

### Scoring

At the conclusion of the game (typically 20 rounds per player), points are tallied:
- **Wildlife Cards:** Each species scores based on specific pattern cards (e.g. pairs of bears, lines of elks, clusters of salmon, dispersed hawks, fox adjacencies).
- **Habitats:** For each of the five habitat types, players score 1 point per tile in their single largest contiguous area.
- **Majority Bonuses:** The player with the largest area in each habitat type earns bonus points.
- **Nature Tokens:** Each unused nature token awards 1 point.

The player with the highest total score wins.

---

## Controls and Navigation

- **Left Mouse Click:** Select market items, buttons, and board placement positions.
- **Rotate Tile:** Click the rotation buttons in the draft area to rotate your selected tile 60 degrees.
- **Pan Board:** Click and drag on the board surface or use the on-screen navigation controls.
- **Zoom:** Use the mouse scroll wheel or on-screen zoom buttons to scale the view.
- **Player Board Switching:** Use the player switcher tabs in the top bar to inspect opponents' boards.
- **Rules and Help:** Click the Help button at any time during play to open the reference guide.

---

## Project Structure

```text
Projekt2/
├── gradle/                  # Gradle wrapper files
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   ├── Main.kt      # Application entry point
│   │   │   ├── entity/      # Domain model and data classes
│   │   │   ├── service/     # Business logic, bot AI, and networking
│   │   │   │   ├── bot/     # AI strategy and decision engine
│   │   │   │   └── network/ # BGW network client and protocol handling
│   │   │   └── gui/         # BGW scenes, visual components, and renderers
│   │   └── resources/       # Visual assets, textures, and sounds
│   └── test/
│       └── kotlin/          # Unit tests and test suites
├── build.gradle.kts         # Gradle build configuration
├── settings.gradle.kts      # Project settings and plugin management
└── README.md                # Project documentation
```

---

## Technologies and Dependencies

- **Language:** Kotlin 2.3.0
- **Runtime:** JVM (Target Java 11)
- **GUI & Game Framework:** [Board Game Workstation (BGW)](https://github.com/tudo-aqua/bgw) by TU Dortmund
  - `bgw-gui`: Graphical scenes, layout, and event handling
  - `bgw-net-client` & `bgw-net-common`: Network client protocol for multiplayer sessions
- **Network Protocol:** `edu.udo.cs.sopra:ntf` (Network Transfer Format)
- **Serialization & JSON:** Jackson (`jackson-module-kotlin`)
- **Testing:** JUnit 5 with `kotlin.test`
