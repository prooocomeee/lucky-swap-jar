# Lucky Swap (Fabric)

Server-side Minecraft mod. The server distributes Lucky Blocks and periodically swaps online players.

## Build
- Java 25
- Gradle (use the Gradle version supported by Fabric Loom)
- Run `gradle build`
- Output: `build/libs/lucky-swap-1.0.0.jar`

## Install
Install the matching Fabric Loader and Fabric API on the server, then place the mod JAR in `mods/`.
Clients do not need the mod for a dedicated server.

## Important
The current implementation uses yellow concrete as the Lucky Block marker. Consequently, naturally
placed yellow concrete is also treated as a Lucky Block when broken. Use this only in a world where
that behavior is acceptable.
