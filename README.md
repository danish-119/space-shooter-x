# Space Shooter X - 2D Game

## 🚀 Overview

**Space Shooter X** is a sophisticated 2D space shooter game built in Java with advanced graphics, AI systems, and dynamic gameplay mechanics. The game features procedural audio, particle effects, multiple enemy types with advanced AI, and a comprehensive weapon system.

![Game Features](docs/banner.png)

---

## 🎮 Key Features

### Core Gameplay
- **Advanced Enemy AI** with 7 behavioral states and movement patterns
- **Dynamic Difficulty System** with custom configuration options
- **Multiple Weapon Types** (4 primary weapons with unique mechanics)
- **Power-up System** with time-based effects and visual feedback
- **Progressive Leveling** with increasing challenge and enemy variety
- **Score & Combo System** with multipliers and streak tracking

### Visual Effects
- **Particle System** with 10+ particle types and object pooling
- **Screen Effects** including shake, flash, and zoom transitions
- **Dynamic Trails** for entities and projectiles
- **Crystal-Style Player Bullets** with multiple visual styles (laser, plasma, rail, spread, charged)
- **Explosion Effects** with size and intensity variations
- **Damage Overlays** and shield visualization
- **Fullscreen Support** with letterboxing for any resolution

### Audio System
- **Procedural Audio** - All sounds generated dynamically (no external files)
- **20+ Sound Types** with volume and pitch variations
- **Multi-threaded Audio Engine** with sound pooling
- **Dynamic Audio** responding to game events and intensity

---

## 🏗️ Architecture Overview

### Core Components

```
src/main/java/com/spaceshooterx/
├── core/           # Game engine and management
├── entities/       # Game objects and actors
├── effects/        # Visual and audio effects
├── audio/          # Sound generation and management
└── util/           # Utility classes and helpers
```

### Design Patterns Used
- **Singleton Pattern**: Resource managers (ParticleSystem, SoundManager)
- **Strategy Pattern**: Enemy AI states and weapon behaviors
- **Factory Pattern**: Entity creation and spawning
- **Observer Pattern**: Game event handling
- **Object Pooling**: Performance optimization for particles and sounds

---

## 📁 Detailed Class Documentation

### Core System (`com.spaceshooterx.core`)

#### `Main.java`
- **Purpose**: Application entry point
- **Responsibilities**: Window creation, game initialization, event handling

#### `ShootingGame.java` 
- **Purpose**: Main game engine and render loop
- **Key Features**:
  - 60 FPS game timer with delta time calculations
  - Comprehensive collision detection system
  - Thread-safe entity management with `CopyOnWriteArrayList`
  - Fullscreen support with automatic letterboxing
  - Multi-layered rendering pipeline
- **Methods**:
  - `paintComponent()`: Main rendering pipeline
  - `updateGameLogic()`: Core game state updates
  - `handleCollisions()`: Collision detection and response
  - `spawnEnemyByType()`: Dynamic enemy creation based on level

#### `GameConfig.java`
- **Purpose**: Centralized game configuration
- **Contains**: 
  - Display settings (dimensions, colors)
  - Entity properties (sizes, speeds, health)
  - Gameplay parameters (spawn rates, difficulty scaling)
  - Performance settings (max entities, frame rates)

#### `Difficulty.java`
- **Purpose**: Dynamic difficulty management
- **Difficulty Levels**: EASY, NORMAL, HARD, EXPERT, NIGHTMARE, CUSTOM
- **Features**:
  - Scalable enemy properties (health, damage, speed)
  - Adaptive spawn rates and cooldowns
  - Custom difficulty configuration support
  - Progressive difficulty scaling with level

#### `GameState.java`
- **Purpose**: Game state management
- **States**: MENU, PLAYING, PAUSED, GAME_OVER, SETTINGS
- **Features**: State transition handling and persistence

---

### Entity System (`com.spaceshooterx.entities`)

#### `Player.java`
- **Purpose**: Player character with comprehensive control system
- **Advanced Features**:
  - **Multi-Weapon System**: 4 weapon types (Laser Cannon, Plasma Burst, Spread Gun, Beam Laser)
  - **Power-up Management**: Thread-safe power-up tracking with `ConcurrentHashMap`
  - **Shield System**: Regenerating shields with visual feedback
  - **Movement Physics**: Smooth acceleration/deceleration with bounds checking
  - **Advanced Rendering**: Engine flames, damage states, weapon effects
- **Key Methods**:
  - `update()`: Physics and input processing
  - `shoot()`: Weapon firing with type-specific behavior
  - `activatePowerUp()`: Power-up effect application
  - `takeDamage()`: Damage processing with shield interaction

#### `Enemy.java` (Abstract Base Class)
- **Purpose**: Advanced enemy AI foundation
- **AI States**: 
  - `SPAWNING`: Entry animation and setup
  - `PATROLLING`: Movement pattern execution
  - `ATTACKING`: Combat engagement
  - `CHARGING`: Preparation for special attacks
  - `EVADING`: Defensive maneuvering
  - `FLEEING`: Retreat behavior
  - `DYING`: Death animation and cleanup
- **Movement Patterns**:
  - `SINUSOIDAL`: Wave-based movement
  - `ZIGZAG`: Sharp directional changes
  - `CIRCULAR`: Orbital patterns
  - `PATROL`: Waypoint-based movement
- **Advanced Features**:
  - Predictive targeting with accuracy simulation
  - Dynamic difficulty scaling
  - Visual effects integration (trails, damage overlays)
  - Comprehensive combat system with cooldowns
  - Formation flying capabilities

#### `Enemy1.java` - Scout Class
- **Role**: Fast reconnaissance unit
- **Characteristics**:
  - High speed, low health (50 HP)
  - Sinusoidal movement pattern
  - Basic laser attacks (10 damage)
  - 50 point score value
- **AI Behavior**: Hit-and-run tactics with evasive movement

#### `Enemy2.java` - Fighter Class  
- **Role**: Balanced combat unit
- **Characteristics**:
  - Medium speed and health (100 HP)
  - Burst fire capability (3-round bursts)
  - Charging attack patterns
  - 75 point score value
- **Special Abilities**:
  - Charge attacks with warning indicators
  - Rapid-fire mode when player is close
  - Predictive shooting algorithms

#### `Enemy3.java` - Bomber Class
- **Role**: Heavy assault unit
- **Characteristics**:
  - Low speed, high health (150 HP)
  - Guided bomb deployment
  - Formation flying patterns
  - 200 point score value
- **Special Abilities**:
  - Guided bomb system with target prediction
  - Heavy armor with shield systems
  - Bombing run coordination

#### `Bullet.java`
- **Purpose**: Player projectile system
- **Features**:
  - Multiple bullet types with unique properties
  - Trail effects and impact visuals
  - Configurable damage and speed values

#### `EnemyBullet.java`
- **Purpose**: Advanced enemy projectile system
- **Advanced Features**:
  - **Homing Capabilities**: Target tracking and prediction
  - **Explosive Projectiles**: Area damage with particle effects
  - **Bullet Types**: Basic, homing, explosive, guided bombs
  - **Visual Effects**: Dynamic trails, glow effects, prediction lines
- **Special Mechanics**:
  - Player movement prediction algorithms
  - Splash damage calculations
  - Dynamic size and visual scaling

#### `PowerUp.java`
- **Purpose**: Collectible enhancement system
- **Types**:
  - `RAPID_FIRE`: Increased firing rate
  - `SPREAD`: Multi-directional shots
- **Features**:
  - Animated pickup effects
  - Time-based duration system
  - Visual feedback and sound integration

#### `Rock.java`
- **Purpose**: Environmental obstacles
- **Features**:
  - Procedural size variation
  - Collision damage system
  - Dynamic spawning based on level

---

### Effects System (`com.spaceshooterx.effects`)

#### `ParticleSystem.java`
- **Purpose**: Comprehensive particle effect engine
- **Performance Features**:
  - Object pooling (1000 particle pool)
  - Multi-threaded rendering with `CopyOnWriteArrayList`
  - Automatic cleanup and recycling
  - Configurable maximum particle limits (5000 max)
- **Particle Types**:
  - `SPARK`: Quick flash effects
  - `SMOKE`: Trailing smoke effects
  - `FIRE`: Flame animations
  - `ENERGY`: Weapon charge effects
  - `BLOOD`: Impact effects
  - `STAR`: Background ambience
  - `GLOW`: Aura effects
  - `DEBRIS`: Explosion fragments
  - `TRAIL`: Movement trails
  - `EXPLOSION`: Blast effects
- **Advanced Properties**:
  - Physics simulation (gravity, drag, wind)
  - Color interpolation and alpha fading
  - Size scaling and rotation
  - Custom lifecycle management
- **Key Methods**:
  - `createExplosion()`: Various explosion types
  - `createTrail()`: Entity trailing effects
  - `createMuzzleFlash()`: Weapon firing effects
  - `renderImmediate()`: Thread-safe immediate rendering

#### `AnimationManager.java`
- **Purpose**: Screen-level animation and camera effects
- **Screen Effects**:
  - **Screen Shake**: Intensity and duration-based camera shake
  - **Screen Flash**: Color-tinted flash effects for impact
  - **Screen Zoom**: Dynamic zoom transitions
  - **Fade Effects**: Scene transition management
- **Camera System**:
  - Transform matrix management
  - Smooth interpolation between states
  - Viewport scaling and centering
- **Post-Processing**:
  - Color grading and filters
  - Overlay rendering system
  - UI effect coordination

---

### Audio System (`com.spaceshooterx.audio`)

#### `SoundManager.java`
- **Purpose**: Procedural audio generation and management
- **Technical Features**:
  - **Multi-threaded Audio Engine**: Concurrent sound generation
  - **Sound Pooling**: Efficient clip reuse and management
  - **Procedural Generation**: All sounds created algorithmically
  - **Volume Control**: Separate master, music, and SFX volumes
- **Sound Categories**:
  - **Player Actions**: Shooting, death, hit, power-up
  - **Weapons**: Laser, plasma, rocket, railgun, beam
  - **Enemies**: Various shot types and explosions by enemy class
  - **UI**: Click, hover, select, error sounds
  - **Game Events**: Level up, combos, alarms, warnings
  - **Ambient**: Engine hum, space atmosphere
- **Waveform Types**:
  - Sine, Square, Sawtooth, Triangle, Noise, Pulse
- **Audio Properties**:
  - Frequency modulation and harmonics
  - ADSR envelope control (Attack, Decay, Sustain, Release)
  - Dynamic pitch and volume variations
  - Spatial audio simulation

---

### Utility System (`com.spaceshooterx.util`)

#### `ImageUtils.java`
- **Purpose**: Image loading and manipulation
- **Features**:
  - Resource loading with error handling
  - Image resizing and scaling
  - Format conversion and optimization
- **Methods**:
  - `loadImage()`: Resource loading with fallback
  - `resizeImage()`: Efficient image scaling

---

## 🎯 Gameplay Mechanics

### Combat System
- **Player Weapons**: 4 distinct weapon types with unique firing patterns
- **Enemy AI**: Advanced behavioral states with predictive targeting
- **Damage System**: Health, shields, and armor interactions
- **Collision Detection**: Precise bounds-based collision system

### Progression System
- **Dynamic Difficulty**: Automatic scaling based on player performance
- **Level Progression**: Increasing enemy variety and challenge
- **Score System**: Points with combo multipliers and streaks
- **Power-ups**: Temporary enhancements with visual feedback

### Visual Effects
- **Particle Effects**: Explosions, trails, impacts, and ambient effects
- **Screen Effects**: Camera shake, flash, zoom for enhanced feedback
- **Dynamic Lighting**: Glow effects and color blending
- **Animation System**: Smooth interpolation and state transitions

---

## 🛠️ Technical Specifications

### Performance Optimizations
- **Object Pooling**: Particles, bullets, and sounds reused for efficiency
- **Thread Safety**: Concurrent collections prevent race conditions
- **Efficient Rendering**: Immediate-mode particle rendering
- **Memory Management**: Automatic cleanup and garbage collection optimization

### System Requirements
- **Java Version**: Java 11 or higher
- **Memory**: Minimum 512 MB RAM
- **Graphics**: Hardware acceleration recommended
- **Audio**: Java Sound API support

### Code Quality
- **Design Patterns**: Professional software architecture
- **Error Handling**: Comprehensive exception management
- **Documentation**: Extensive inline comments and javadoc
- **Maintainability**: Modular design with clear separation of concerns

---

## 🚀 Installation & Usage

### Compilation
```bash
# Navigate to project directory
cd "Space Shooter X - 2D Game"

# Compile all Java files
javac --release 11 -d out -cp src\main\java src\main\java\com\spaceshooterx\**\*.java

# Run the game
java -cp "out;src\main\resources" com.spaceshooterx.core.Main
```

### Controls
- **Movement**: Arrow Keys or WASD
- **Primary Fire**: Spacebar
- **Secondary Fire**: Ctrl
- **Special Weapons**: Shift (charged special)
- **Special (alternate)**: E (activate when charged)
- **Pause**: P or Escape
- **Fullscreen**: F11
- **Dash**: Z or X

### Configuration
- Difficulty settings can be adjusted in `Difficulty.java`
- Game parameters are configurable in `GameConfig.java`
- Audio settings managed through in-game controls

---

## 🏆 Features Showcase

### Advanced Enemy AI
```java
// Enemy AI state machine with 7 behavioral states
protected enum EnemyState {
    SPAWNING, PATROLLING, ATTACKING, 
    CHARGING, EVADING, FLEEING, DYING
}

// Movement patterns for varied gameplay
protected enum MovementPattern {
    SINUSOIDAL, ZIGZAG, CIRCULAR, PATROL
}
```

### Particle System
```java
// 10+ particle types with advanced physics
public enum ParticleType {
    SPARK, SMOKE, FIRE, ENERGY, BLOOD,
    STAR, GLOW, DEBRIS, TRAIL, EXPLOSION
}
```

### Procedural Audio
```java
// 20+ sound types, all procedurally generated
public enum SoundType {
    PLAYER_SHOOT, ENEMY_EXPLOSION_LARGE,
    LASER_ENHANCED, SCREEN_SHAKE, // ... and more
}
```

---

## 🔧 Development Notes

### Architecture Decisions
- **Entity-Component System**: Modular entity design for flexibility
- **State Machines**: Clear AI behavior definition and debugging
- **Singleton Managers**: Centralized resource management
- **Thread-Safe Collections**: Prevents concurrent modification issues

### Performance Considerations
- **Object Pooling**: Reduces garbage collection pressure
- **Immediate Rendering**: Minimizes render state changes
- **Efficient Collision**: Spatial partitioning for large entity counts
- **Audio Threading**: Non-blocking sound generation

### Extensibility
- **Pluggable Difficulty**: Easy to add new difficulty modes
- **Modular Weapons**: Simple weapon system extension
- **Effect Framework**: Easy particle and animation addition
- **AI Framework**: Straightforward enemy type creation

---

## 📊 Statistics

| Component | Lines of Code | Key Features |
|-----------|---------------|--------------|
| **Core System** | ~2000 | Game loop, collision, rendering |
| **Enemy AI** | ~1500 | Advanced AI, multiple enemy types |
| **Player System** | ~1400 | Weapons, movement, power-ups |
| **Particle System** | ~1200 | Effects, pooling, threading |
| **Audio Engine** | ~1000 | Procedural generation, threading |
| **Effects Manager** | ~800 | Screen effects, animations |
| **Utilities** | ~200 | Image handling, configuration |
| **Total** | **~8100** | Professional game architecture |

---

## 🎮 Conclusion

**Space Shooter X** demonstrates professional game development practices with:
- **Sophisticated Architecture**: Enterprise-level design patterns
- **Advanced Graphics**: Modern particle and animation systems  
- **Intelligent AI**: Behavioral state machines with dynamic difficulty
- **Optimized Performance**: Object pooling and multi-threading
- **Extensible Design**: Easy to modify and enhance

This project serves as an excellent example of advanced Java game programming with modern software engineering practices.

---

*© 2026 Space Shooter X - Advanced 2D Game Development*