# Spectre for Minecraft 1.21.11

An unofficial **backport of the [Spectre](https://github.com/cody-raves/Spectre) Fabric client to Minecraft 1.21.11**, with a redesigned "Spectral" interface (violet/ectoplasm palette, frosted-glass panels, spring animations).

> **Credit.** Spectre was created by Cody ([cody-raves](https://github.com/cody-raves)) and builds on existing open-source work (see `src/main/resources/GONUTS-LICENSE.txt` and `LICENSE_spectre`). The original targets Minecraft 26.2; this repository is a **fork-style port** and is not affiliated with or endorsed by the original author. Licensed **GPL-3.0-only**, like the original; the full source is in this repository.
>
> Before using it, read the original project's behaviour and security report: [UPSTREAM-SECURITY-REPORT.md](UPSTREAM-SECURITY-REPORT.md). Notably, the account switcher stores login tokens **unencrypted** in `config/spectre/accounts.json`. Don't add your main account.

## Download

**[⬇ Latest release](../../releases/latest)** → download `spectre-0.9.1+mc1.21.11.jar` (not the `-sources` file).

Verify it if you like. SHA-256 of the current build:

```
115e0ab28ede8c6ac42cfd50537e86932a125efb7a8f5e06f50edcd605a7e315  spectre-0.9.1+mc1.21.11.jar
```

```powershell
Get-FileHash -Algorithm SHA256 .\spectre-0.9.1+mc1.21.11.jar
```

## Install the mod

You need **Minecraft 1.21.11**, **Fabric Loader 0.18.0 or newer**, **Fabric API 0.141.0+1.21.11 or newer**, and **Java 21** (the Minecraft launcher bundles it).

1. Install Fabric for 1.21.11 (Fabric installer, or create a Fabric 1.21.11 instance in Modrinth/Prism).
2. Put **Fabric API** in the instance's `mods` folder.
3. Put `spectre-0.9.1+mc1.21.11.jar` in the same `mods` folder.
4. Launch the game and press **Right Shift** to open Spectre.

Use a **separate instance** from the 26.2 version of Spectre: both have the mod id `spectre`, and Fabric refuses to load two.

### Compatibility notes
- Works alongside Sodium, Iris, Lithium and similar mods (tested on a profile with them). With **Sodium**, Freecam/Freelook don't change vanilla culling, because Sodium replaces those methods.
- Not compatible with: Embeddium, OptiFabric, Canvas, VulkanMod (declared in `fabric.mod.json`).
- The in-game media player downloads a Chromium runtime the first time you open it.

## Build from source

Requires a **JDK 21** and internet access for the first build (Gradle, Minecraft and dependencies are downloaded).

```bash
git clone https://github.com/maximumemails-cmd/spectre-1.21.11.git
cd spectre-1.21.11

# Windows (PowerShell) - point JAVA_HOME at your JDK 21 first
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21"
.\gradlew.bat build

# Linux / macOS
export JAVA_HOME=/path/to/jdk-21
./gradlew build
```

The mod JAR is written to `build/libs/spectre-0.9.1+mc1.21.11.jar` (the `-sources.jar` next to it is only the source archive).

To run a development client with the mod loaded: `./gradlew runClient`.

## What differs from upstream

- Retargeted to Minecraft 1.21.11 (Java 21, Fabric Loom with Mojang mappings).
- GUI, render-pipeline and mixin code ported from the 26.x APIs. See [PORTING-1.21.11.md](PORTING-1.21.11.md) for details, known behaviour differences and a test order.
- New "Spectral" UI: palette, glass blur backdrop, springs, colour picker, restyled HUD, toasts and menus.
- The friends-service part of the account switcher doesn't exist in 1.21.11 and was removed.

## Status

Compiles, and every mixin target was checked against the real 1.21.11 classes. It loads in a single-player world. Not everything has been play-tested, so expect rough edges; please open an issue with your `latest.log`.

## License

GPL-3.0-only. See [LICENSE](LICENSE) and the notices in `src/main/resources`.
