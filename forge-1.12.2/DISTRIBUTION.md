# GTM Advanced Terminal 1.12.2 Distribution

Version: `1.0.0-forge-1.12.2`.

This is a third-party derivative of GTMThings, not an official release.
Upstream author: liansishen. The terminal code and texture authorization
is recorded at https://github.com/liansishen/GTMThings/issues/127#issuecomment-5870569378.
See `NOTICE` for the source and modification summary, `LICENSE` for LGPLv3,
and `COPYING` for the GPLv3 text incorporated by LGPLv3.

## Sharing

Provide these files together from the same download location or directly
to each recipient:

- `gtmadvancedterminal-1.0.0-forge-1.12.2.jar`
- `gtmadvancedterminal-1.0.0-forge-1.12.2-sources.zip`
- This distribution guide, `LICENSE`, `COPYING` and `NOTICE` (also included
  in the JAR and source archive).

The source archive contains the source, resources, Gradle wrapper and build
configuration used for this release. It excludes caches, game files,
private local paths in handoff documents and third-party dependency JARs.
Keep the matching archive available when sharing the JAR. Recipients may
modify and redistribute this derivative under LGPLv3.

## Build From Source

Requirements: JDK 8, Gradle 4.9 (included wrapper), ForgeGradle 3.0.197,
Minecraft 1.12.2 / Forge 14.23.5.2860, MCP stable 39-1.12 mappings.
The local project was built offline using an existing Gradle cache.
A fresh environment needs network access to obtain Forge/Gradle artifacts.

Create a `libs` directory and obtain these separate compile dependencies
from their respective projects (they are not embedded in the output JAR):

| File | Project / download location |
| --- | --- |
| `gregtech-1.12.2-2901.jar` | https://github.com/GregTechCEu/GregTech |
| `modularui-3.0.6.jar` | https://www.curseforge.com/minecraft/mc-mods/modularui |
| `ae2-uel-v0.56.7.jar` | https://github.com/AE2-UEL/Applied-Energistics-2 |
| `CodeChickenLib-1.12.2-3.2.3.358-universal.jar` | https://www.curseforge.com/minecraft/mc-mods/codechicken-lib-1-8 |
| `EnderCore-1.12.2-0.5.78.jar` | https://www.curseforge.com/minecraft/mc-mods/endercore |
| `annotations-13.0.jar` | Gradle 4.9 distribution, `lib/annotations-13.0.jar` |

Copy the annotation JAR from the unpacked Gradle 4.9 distribution to `libs`.
Set `JAVA_HOME` to your JDK 8 installation and add its `bin` to `PATH`.
Edit `org.gradle.java.home` in `gradle.properties` to your own JDK 8 path.
Optionally set `GRADLE_USER_HOME` to your own Gradle cache directory.
Run on Windows:

```powershell
.\gradlew.bat build sourceDistribution --console=plain
```

Add `--offline` when all dependencies and the wrapper are already cached.
The local verification uses ForgeGradle 3.0.197; the existing buildscript
uses `3.+`, so a fresh cache may resolve a different 3.x version. To use the
same tool version, replace that dependency version with `3.0.197`.

Outputs: the reobfuscated JAR in `build/libs` and `mods输出`, and the source
ZIP in `mods输出`. Runtime requires GTCEu, ModularUI, AE2 and the dependencies
required by those mods. Bubbles/baubles integration is optional.

## Testing Status

The current release has build and archive checks only. Full gameplay
verification in GT Lite remains pending. Share it as a test build until
auto-building, staged optional structures, mirror layouts, coil replacement
and AE supply have been tested in-game. No game installation is performed
by the build.
