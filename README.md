# RaindanceMC/ctf

- **`ctf-module`** — Rain's Flexible Modules — Capture the Flag: Capture the Flag on maps built from schematics: classic, one flag and neutral flag rounds, classes, power-ups, mutators, votes, awards, levels and daily challenges.
- **`ctf-standalone`** — Rain's Capture the Flag: Rain's Capture the Flag, as a plugin of its own: the ctf module and the standard wrapper, shaded together.

Part of [RaindanceMC](https://github.com/RaindanceMC); built on [RainsCore](https://github.com/RaindanceMC/RainsCore) and the shared parent in [framework](https://github.com/RaindanceMC/framework). Split out of `rains-flexible-modules` on 16.10.2026, history kept.

## Build

```
mvn install
```

Needs Java 25 and read access to `packages.tstieh.de` (server ids `packages-releases` / `packages-snapshots` in `~/.m2/settings.xml`).

## Plugins

- `ctf-standalone/target/RainsCaptureTheFlag-0.1.0.jar` — also published as `de.raindancer.modules:ctf-standalone:0.1.0`

## Versions

A module's version is a property in this repository's root `pom.xml`, a plugin's is `plugin.version` in its own pom. CI publishes every version that is not in the registry yet on the next push to master.
