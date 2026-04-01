# Space Shooter X - Technical Documentation & README

**Space Shooter X** is a high-performance 2D arcade-style space shooter developed in Java. The engine emphasizes smooth 60 FPS gameplay, dynamic difficulty scaling, and a sophisticated modular bullet pattern system.

---

## 🎮 Game Overview
Players command a customizable starship against waves of enemies and environmental hazards. The core gameplay loop focuses on high-speed combat, score-based progression, and tactical switching between unlocked firing patterns.

### Key Features
* **Dynamic Difficulty System**: Automatically scales enemy spawn rates, health, damage, and projectile speed across three tiers: EASY, NORMAL, and HARD.
* **Modular Bullet Patterns**: Instead of fixed weapon types, the player unlocks up to 8 distinct geometric firing patterns (Single, Triangle, Arrow, Diamond, Pentagon, Cross, Inverse Triangle, Hexagon) based on their current level.
* **Procedural Visual Effects**: Features a custom particle engine for explosions, muzzle flashes, and engine trails, alongside screen-space effects like camera shake and damage flashes.
* **Real-time Radar**: A functional HUD-integrated radar that tracks enemy positions relative to the player's ship.
* **Adaptive Audio**: Procedurally manages engine hums, combat sound effects, and UI feedback through a centralized Sound Manager.

---

## 🏗️ Technical Architecture

### Core Systems (`com.spaceshooterx.core`)
* **ShootingGame.java**: The primary engine containing the 60 FPS game loop, collision detection, and multi-layered rendering pipeline.
* **GameConfig.java**: Centralized configuration for all constants, including screen dimensions ($1200 \times 800$), entity speeds, and scoring tiers.
* **Difficulty.java**: Manages game balancing, defining multipliers for enemy stats and power-up durations.

### Entity Framework (`com.spaceshooterx.entities`)
* **Player.java**: Handles physics-based movement (acceleration/deceleration), invulnerability states, and the firing pattern logic.
* **Enemy.java**: Provides the base AI for hostiles, supporting various movement patterns like Sinusoidal, Zigzag, and Circular.
* **Bullet/EnemyBullet**: Distinct projectile classes for the player and AI, supporting custom damage values and visibility tracking.

---

## 🛠️ Controls & Mechanics

### Movement & Combat
* **Move**: Arrow Keys or WASD.
* **Primary Fire**: Spacebar (Hold for continuous fire).
* **Switch Patterns**: Number keys **1–8** (Unlocks progressively as you level up).
* **Pause**: P or Escape.

### Pattern System
Each pattern distributes the ship's total damage across its projectiles. While higher patterns provide wider coverage, the damage is split among more bullets, requiring players to choose the right pattern for the situation (e.g., Single for bosses, Hexagon for crowds).

### Power-Ups
* **Heart (H)**: Restores player health.
* **Shield (S)**: Grants temporary protection against one-hit damage.
* **Rapid Fire (RF)**: Halves the weapon cooldown duration.
* **Spread Shot (SP)**: Temporarily activates the widest possible firing pattern.
* **Pattern Upgrade (PU)**: Permanently increases the player's pattern level for the current session.

---

## 🚀 Installation & Running

### Prerequisites
* **Java**: JDK 11 or higher.

### Compilation
From the project root:
```bash
javac -d bin -cp src/main/java src/main/java/com/spaceshooterx/core/Main.java
```

### Execution
```bash
java -cp "bin;src/main/resources" com.spaceshooterx.core.Main
```

---

## 📊 Game Statistics
| Feature | Implementation Detail |
| :--- | :--- |
| **Target FPS** | 60 FPS |
| **Max Enemies** | 5–10 (Difficulty dependent) |
| **Max Level** | Infinite (Difficulty caps at Level 20+) |
| **Screen Resolution** | $1200 \times 800$ pixels |
| **Score System** | Base 25 points per enemy with combo multipliers |

*© 2026 Space Shooter X - Advanced 2D Game Development*
