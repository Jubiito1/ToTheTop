# To The Top

---

## Team Members
- Julian Guido Bollinger
- Santino Gennuso Musmanno
- Tomás Raiter

---

## Short Game Description
**To The Top** is a 2D front-view game for 1 to 4 players,
focused on physics-based climbing, with both single-player
and cooperative multiplayer modes. Players must climb a
volcanic mountain made up of different sections and obstacles
to reach the summit before an eruption occurs, using a
movement system based on limb control and physical
interaction (ragdoll) with the environment and with other
players. The project's main references and inspirations are
*Getting Over It*, *Peak*, and *Mount Your Friends*.

---

## Main Technologies and Target Platforms
- Java 21
- LibGDX 1.14.1
- Additional tools: Box2D (physics) and Tiled (map design)
- Target platform: Desktop (Windows and Linux)
- IDE used: IntelliJ IDEA Community Edition

---

## Link to the Project Wiki (Detailed Proposal)

[See the full project proposal here](https://github.com/Jubiito1/ToTheTop/wiki)

---

## How to Build and Run
### Windows and Linux

1. **Clone the repository**

```bash
   git clone https://github.com/Jubiito1/ToTheTop
   cd ToTheTop
```

2. **Import the project into a Gradle-compatible IDE (IntelliJ recommended)**
    - Select as a Gradle project
    - Verify that Java 21 is being used

3. **Run the game**
    - From the IDE, run the `Lwjgl3Launcher.java` class located in the `lwjgl3` module
    - Or from the console (Linux and Windows, using gradlew):

```bash
   # Linux
   ./gradlew lwjgl3:run

   # Windows (CMD or PowerShell)
   gradlew.bat lwjgl3:run
```
