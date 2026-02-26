# MSF - Make Skyblock Fun

A Fabric mod for Minecraft 1.21.1 that automates fishing in Hypixel SkyBlock.

## Features

### Automated Fishing Loop
Full cast-wait-reel-recast cycle. The macro handles rod selection, casting, bite detection, reeling, and re-casting automatically via a 12-state state machine.

### Sea Creature Combat
Detects sea creatures that spawn while fishing and automatically fights them. Supports two combat modes:
- **Hyperion mode** - Looks down and uses Wither Impact AoE ability (configurable retry attempts)
- **Melee mode** - Walks toward the creature and attacks with randomized CPS

### Humanized Behavior
All actions use randomized timing and smooth easing to mimic human input:
- Randomized delays on every action (cast, reel, react, attack)
- Smooth camera rotation with `easeOutBack` easing
- 30% chance of slight overshoot on rotations with automatic correction
- Humanized aiming that targets random points within a creature's hitbox
- Random sprint toggling during melee combat

### Anti-AFK
Periodic small camera movements while waiting for bites to avoid AFK detection:
- Randomized intervals (15-30s default)
- Tiny yaw/pitch drift with bias back toward center
- 15% chance to skip a movement entirely

### Knockback Recovery
Detects when the player gets knocked away from their fishing spot:
- Uses Baritone pathfinding to return if available
- Falls back to simple walk-back if Baritone is not installed
- Restores original camera rotation after returning

### Configuration
All parameters are saved to `~/.minecraft/config/fishingmacro.json` and can be edited directly:

| Setting | Default | Description |
|---------|---------|-------------|
| `rodSlot` | 1 | Fishing rod hotbar slot (0-8) |
| `weaponSlot` | 0 | Weapon hotbar slot (0-8) |
| `useHyperion` | true | Use Hyperion ability vs melee |
| `killTimeoutMs` | 10000 | Max combat duration (ms) |
| `hyperionMaxAttempts` | 3 | Max Hyperion ability uses |
| `hyperionRetryDelayMs` | 500 | Delay between Hyperion retries (ms) |
| `meleeCpsMin` / `Max` | 8 / 12 | Melee attack speed range |
| `reelDelayMinMs` / `Max` | 150 / 400 | Bite reaction delay range (ms) |
| `castDelayMinMs` / `Max` | 150 / 350 | Re-cast delay range (ms) |
| `antiAfkMinIntervalMs` / `Max` | 15000 / 30000 | Anti-AFK movement interval (ms) |
| `antiAfkMaxYawDrift` | 3.0 | Max yaw drift (degrees) |
| `antiAfkMaxPitchDrift` | 1.5 | Max pitch drift (degrees) |
| `knockbackThreshold` | 3.0 | Distance to trigger return (blocks) |
| `knockbackReactionMinMs` / `Max` | 200 / 600 | Knockback reaction delay (ms) |
| `seaCreatureDetectionRadius` | 10.0 | Sea creature scan radius (blocks) |
| `rotationBaseTimeMs` | 400 | Base camera rotation time (ms) |

## Requirements

- Minecraft 1.21.1
- Fabric Loader >= 0.16.0
- Fabric API
- Java 21

## Building

```bash
./gradlew build
```

The built jar will be in `build/libs/`.

## Usage

1. Install the mod jar into your `.minecraft/mods` folder
2. Set the toggle keybind in Minecraft controls settings (category: Fishing Macro)
3. Stand at your fishing spot with rod and weapon in the configured hotbar slots
4. Press the toggle key to start/stop

## License

See `LICENSE` for details.
