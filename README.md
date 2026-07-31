# Rainbow Dye

A Fabric mod for **Minecraft 26.2** that adds one dye and one thing to use it on.

**Rainbow Dye** — crafted from the four primary dyes on the edges of the grid with a
Nether Star in the middle. Right-click any placed shulker box with it and the box cycles
through colours exactly like a `jeb_` sheep, keeping its contents, custom name, and
facing. The dye's name renders cyan (`Rarity.RARE`).

It does **not** work on wool, sheep, beds, candles, banners, or anything else. That is
deliberate: the item is a plain `Item`, not a `DyeItem`, so none of the vanilla dye
plumbing ever sees it.

## Recipe

```
      [ ]  [Red]  [ ]
   [Blue] [Star] [Yellow]      ->  4x Rainbow Dye
      [ ] [Green] [ ]
```

Yield lives in `data/rainbowdye/recipe/rainbow_dye.json` (`result.count`).

## The jeb_ cycle

| | jeb_ sheep | this box |
|---|---|---|
| palette | 16 DyeColor wool colours | same 16, same ordinal order |
| dwell per colour | 25 ticks | `frametime: 25` |
| full loop | 400 ticks / 20 s | 400 ticks / 20 s |
| blending | lerps adjacent colours | `interpolate: true` |
| phase offset | staggered per entity | none - all boxes sync (texture ticker is global) |

The palette faithfully includes gray, light gray, brown, and black, so ~1/4 of the loop
is muddy. To skip them, drop those frames from the texture and set `frametime: 33`.

## Toolchain (26.2 - important)

Minecraft 26.1+ ships **unobfuscated**; Yarn is retired. This project therefore:

- uses real Minecraft class names (`Identifier`, `CreativeModeTabEvents`, ...) with
  **no mappings line** in `build.gradle`
- needs **Java 25** (bytecode level of the 26.x jars)
- pins **Gradle 9.5.1** via the committed wrapper and **Loom 1.17-SNAPSHOT**
  (plugin id `net.fabricmc.fabric-loom` - note the new full id)
- declares Fabric deps with plain `implementation`, not `modImplementation`

All versions mirror `FabricMC/fabric-example-mod` branch `26.2`. A jar built for
1.21.x will crash on 26.2 with `ClassNotFoundException: net.minecraft.class_....` -
those are retired intermediary names. Delete any old build from `mods/` first.

## Repo layout

`build.gradle`, `settings.gradle`, `gradlew`, `src/`, `.github/` must sit at the
**repository root**. If the repo front page shows a folder you have to click into
first, CI fails with "does not contain a Gradle build".

## Building

`./gradlew build` with JDK 25. Jar lands in `build/libs/`. Needs Fabric API on both
client and server.

## Design notes

- **Plain Item, one override.** `useOn` handles shulker boxes and passes on everything
  else, so wool/sheep/beds are structurally unreachable.
- **Block renders as a normal model** (`RenderShape.MODEL`) so the rainbow comes from an
  animated 16-frame texture instead of the shulker block-entity renderer and its
  hard-coded DyeColor table. Trade-off: no lid-opening animation.
- **Contents transfer** rides the item-component path: contents + custom name go into a
  temporary stack (`DataComponents.CONTAINER` / `CUSTOM_NAME`) applied with
  `applyComponentsFromItemStack`, which is public on 26.2 - unlike any name setter.
- **One mixin**, an accessor widening `BlockEntityType.SHULKER_BOX`'s `validBlocks` set
  so chunk reloads don't delete the block entity. Field name verified against Fabric
  API's own accessor on the 26.2 branch. No refmap needed - 26.x mixins target real
  names.

## Verified vs. assumed

Nearly every API call was checked against the Fabric API `26.2` branch source. Not
directly verifiable there, flag if the compiler objects:
`ShulkerBoxBlock`'s `(DyeColor, Properties)` constructor order,
`SoundEvents.DYE_USE`, `ServerLevel.sendParticles`, `Item.Properties#rarity`,
and the `minecraft:copy_components` loot function name.
