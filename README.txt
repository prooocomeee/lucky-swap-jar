Lucky Swap (Minecraft 26.3, Fabric)

- Every 10 minutes: 3-2-1 countdown, then online players swap locations.
- Every 10 seconds: each online player receives a Lucky Block.
- Breaking yellow concrete triggers a random reward/effect.

Build requirements:
- Java 25 (the project compiles with Java release 25)
- Gradle compatible with Fabric Loom
- Run: gradle build
- Output: build/libs/lucky-swap-1.0.0.jar

Install the matching Fabric Loader and Fabric API on the server and put the JAR in mods/.
Clients do not need the mod on a dedicated server.

NOTE: yellow concrete is currently used as the marker, so all yellow concrete blocks are treated
as Lucky Blocks when broken.
