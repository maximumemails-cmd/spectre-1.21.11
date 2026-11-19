# Spectre 0.9.1 → Minecraft 1.21.11 backport

Branch: `port/1.21.11`. Module: `Spectre-1.21.11/` (standalone Gradle project; the original
`Spectre-26.2/` module is untouched).

## Status

| Stage | State |
|---|---|
| Build retargeted (Java 21, Loom `fabric-loom` + Mojang mappings, Fabric API 0.141.6+1.21.11, MCEF 0.3.3+mc1.21.11) | Done |
| `compileJava` | **Passes (0 errors)**, down from 765 |
| Mixin targets, method selectors, `@At` targets, `@Shadow`/`@Accessor`/`@Invoker` checked against the real 1.21.11 classes | **All pass** (151 targets, 235 selectors, 87 injection points, 83 member refs) |
| Remapped release JAR (`build/libs/spectre-0.9.1+mc1.21.11.jar`) | Builds; mixin strings remapped to intermediary |
| Handler signatures and injection points checked inside each target method (229 injectors) | **All pass** |
| In-game testing | Loads into a single-player world alongside Sodium, Iris, Lithium and other performance mods. Most features are still untested. |

## Build

```powershell
cd Spectre-1.21.11
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
& "$env:LOCALAPPDATA\Spectre\tools\gradle-9.5.1\bin\gradle.bat" build
```

Output: `Spectre-1.21.11/build/libs/spectre-0.9.1+mc1.21.11.jar`.

## Install for testing

Use a **separate** 1.21.11 Fabric instance (for example a new Modrinth/Prism profile, or a
separate game directory in the vanilla launcher). Do not put this JAR in the same `mods` folder
as the 26.2 Spectre JAR: both have the mod id `spectre` and Fabric will refuse to start.

Requirements: Minecraft 1.21.11, Fabric Loader 0.18+, Fabric API 0.141+ for 1.21.11, Java 21.

## What changed, by area

**Renames that were 1:1 (mechanical).**
`GuiGraphicsExtractor` → `GuiGraphics` (`text` → `drawString`, `item` → `renderItem`, …);
26.x `Hud` → 1.21.11 `Gui`; `extract*` → `render*` on screens and HUD; the 26.x screen-manager
`Gui` folds back into `Minecraft` (`mc.screen`, `mc.setScreen`, `mc.getOverlay()`,
`mc.getToastManager()`); `ChunkPos` record accessors → fields; `EntityTypes` → `EntityType`;
`ContainerInput` → `ClickType`; Fabric `KeyMappingHelper` → `KeyBindingHelper`,
`ResourceLoader.registerReloadListener` → `registerReloader`, `Screens.getWidgets` → `getButtons`.

**Rendering (26.x WebGPU-style API → 1.21.11 RenderPipeline).**
Bind-group layouts, colour-target and depth-stencil states and primitive topology became the
1.21.11 builder calls (`withSampler`, `withUniform`, `withBlend`, `withColorWrite`,
`withDepthTestFunction`, `withVertexFormat`). `GpuFormat` → `TextureFormat`,
`RenderPass.draw/drawIndexed` and texture uploads use the 1.21.11 argument orders, and buffer
mapping goes through `CommandEncoder.mapBuffer`.
26.x uses **reversed-Z depth**: every depth clear was flipped from `0.0` to `1.0`.

**Compatibility helpers added** (`net.spectre.client.util`):
- `FeatureCompat`: renders an arbitrary `SubmitNodeStorage` (26.x `renderAllFeatures(storage)`).
- `DiscardBufferSource`: used by Glow ESP to reproduce 26.x outline-only feature execution.
- `RenderMatrices`: frame view/projection matrices captured from `LevelRenderer.renderLevel`
  (26.x read them from `CameraRenderState`).
- `AttackPackets`: detects attacks inside `ServerboundInteractPacket` (26.x had `ServerboundAttackPacket`).

**Mixins retargeted.** Examples: `Camera.alignWithEntity` → `setup`;
`Camera.calculateFov/HudFov` → `GameRenderer.getFov`; `LightmapRenderStateExtractor` →
`LightTexture.updateLightTexture`; `ClientClockManager` → `ClientLevel.ClientLevelData.getDayTime`;
`LevelExtractor` → `LevelRenderer.renderLevel`; sky extraction `render` → `extractRenderState`;
`Minecraft.renderFrame`/`GpuSurface.present` → `runTick`/`Window.updateDisplay`.

## Compatibility with other mods

`net.spectre.client.mixinplugin.SpectreMixinPlugin` skips `MixinVanillaCullingLevelRenderer`
when Sodium is installed. Sodium overwrites `LevelRenderer.cullTerrain` and
`isSectionCompiledAndVisible`, and Mixin aborts startup on injections into overwritten methods.
With Sodium, Freecam/Freelook therefore do not disable vanilla smart culling, and Sodium's own
occlusion culling applies.

## Known behaviour differences / risks to test first

1. **Glow ESP / shader ESP**: outline-only pass reimplemented, so it needs an in-game check.
2. **Ghost blocks, item/block icons, shader overlay**: now draw through `FeatureCompat`, which
   flushes the shared buffer sources immediately.
3. **Jump Circles** are queued with the entity submissions (26.x used the block-destroy phase).
4. **Sky (Spectre Sky / Atmosphere)** draws into the main render target (1.21.11 has no
   separate sky target).
5. **Lightmap overrides** (Fullbright, X-ray, No Render darkness) hook the option reads in
   `updateLightTexture` by ordinal.
6. **Sneak packet module** now also marks attack packets (1.21.11 sends attacks through the
   same packet constructor).
7. **Account switcher**: 1.21.11 has no friends service, so that part of the 26.x session rebuild
   was removed.
8. **Time Changer** overrides the client overworld day time.
9. `RenderBridge.translucent` now uses the block's chunk render layer.

Every mixin config uses `defaultRequire: 1`. If an injection point still doesn't match at
runtime, the game stops at startup with a Mixin error that names the mixin class.

## Suggested in-game test order

1. Launch to the title screen (all 150 mixins apply; Spectre title screen; Right Shift GUI).
2. Join a single-player world: HUD, click GUI, module list, notifications.
3. Render modules: Glow/Shader ESP, Nametags, X-ray, Fullbright, Spectre Sky, Motion Blur,
   Color Grading, Hand Aura, Jump Circles, Custom FOV/Zoom, Freecam/Freelook.
4. Tools: Hive Map, Schematic preview/Base Builder, Media Player (MCEF download on first use).
5. Multiplayer-only modules on a test server.
