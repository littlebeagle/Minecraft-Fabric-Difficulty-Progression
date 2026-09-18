# Progressive Difficulty

A Fabric mod for Minecraft Java Edition 26.3 that makes survival progressively
more dangerous as the player reaches meaningful world milestones.

## Requirements

- Java 25
- Minecraft 26.3
- Fabric Loader 0.19.5 or newer
- Fabric API 0.160.7+26.3

The Gradle wrapper downloads the remaining build tools and dependencies.

## Development

Build the mod:

```powershell
.\gradlew.bat build
```

Launch a development client:

```powershell
.\gradlew.bat runClient
```

The distributable JAR is written to `build/libs`.

## Implemented features

- Per-world persistent progression state
- Eight progression milestones and difficulty tiers, completed in any order
- A new early milestone for wearing any one piece of iron armour
- Configurable mob-kill and day thresholds
- Tier increase notifications
- `/pdifficulty status`
- `/pdifficulty inspect` for the nearest hostile mob within 16 blocks
- Operator-only `/pdifficulty settier <0-8>` and `/pdifficulty reset`
- Configurable cow, pig, and sheep herd retaliation from Tier 0
- Temporary pursuit, melee damage, species-specific speed, and knockback
- Spawn-time hostile health and damage scaling by tier, depth, and dimension
- Tier-, depth-, and dimension-aware hostile equipment and enchantments
- Existing vanilla equipment is preserved; Progressive Difficulty only fills empty slots
- Equipment is applied once per mob and uses vanilla-style drop chances
- Tier 8 has a greatly increased chance to roll netherite equipment, which never drops from the mob
- Some higher-tier skeletons and pillagers switch to a melee weapon at close range
- Configurable creeper and spider movement-speed progression
- Restrained tier-based damage scaling for retaliating livestock
- Lightweight synchronized cow shove, pig lunge, and sheep headbutt animations
- Vanilla-style arm motion when tactical skeletons and pillagers switch weapons

The configuration file is generated at
`config/progressivedifficulty.json` after the first launch. Equipment rates,
enchantment power, location bonuses, and drop chance are all configurable.

## License

This project is available under the CC0-1.0 license. See `LICENSE`.
