# To The Top

---

## Team Members

* Julian Guido Bollinger
* Santino Gennuso Musmanno
* Tomás Raiter

---

## Short Game Description

**To The Top** is a 2D physics-based climbing game developed in Java using LibGDX and Box2D.

The current prototype focuses on the implementation of the player's physical body and its interaction with the environment. The character is composed of multiple rigid bodies connected through physics joints, creating a ragdoll-like system. Both arms can be independently controlled using the mouse, allowing the player to interact with the environment through physical movement.

The game takes place in a volcanic mountain environment designed using Tiled. The project is planned to support single-player and cooperative multiplayer gameplay, with climbing and grabbing mechanics being developed as part of the game's core movement system.

The project's main references and inspirations are *Getting Over It*, *Peak*, and *Mount Your Friends*.

---

## Current Prototype Features

### Player and Physics

* Physics-based player character implemented using Box2D.
* Ragdoll-like character composed of:

  * Head
  * Torso
  * Upper arms
  * Forearms
  * Hands
  * Legs
* Body parts connected using Box2D revolute joints.
* Angular limits implemented for selected joints.
* Character body-part masses and densities calculated from the player's total weight.
* Real-time physics simulation with gravity.
* Both arms can be controlled independently using the mouse.
* Mouse movement is converted into forces applied to the character's hands.
* Counter-force is applied to the torso when controlling an arm, allowing the force to affect the character's body.

### Environment

* Tiled map integration.
* Map rendering through LibGDX's Tiled map system.
* Static Box2D collision bodies generated from polygonal surface objects defined in the map.
* Player spawn point configured through the Tiled map.

### Interface and Audio

* Main menu.
* Loading screen.
* Pause menu.
* Settings menu.
* Music volume control.
* Sound-effect volume control.
* Shared audio management system.
* Main menu background and music.
* Basic sound effects.
* Fullscreen startup.
* Hidden/captured mouse cursor during gameplay.

### Project Architecture

* Modular LibGDX project using `core` and `lwjgl3` modules.
* Screen-based game architecture.
* Centralized asset management using LibGDX's `AssetManager`.
* Separate packages for entities, input, maps, physics, assets, menus, and core game systems.

### Planned Features

The following features are part of the planned game but are **not yet implemented in the current prototype**:

* Surface grabbing and climbing mechanics.
* Complete climbing gameplay.
* Volcano eruption and time-limit system.
* Complete mountain level.
* Cooperative multiplayer.
* Client-server networking.
* Multiple simultaneous players.
* Player synchronization over the network.
* Final gameplay progression and win/lose conditions.

---

## Second Project Submission – Demonstration Video

A demonstration video showing the current state of **To The Top** for the project's second submission is available below:

[Watch the demonstration video on YouTube](https://youtu.be/dhm4AgkkpyY)

---

## Main Technologies and Target Platforms

* Java 21
* LibGDX 1.14.1
* Box2D
* Tiled
* Gradle
* LWJGL3
* IntelliJ IDEA Community Edition

### Target Platforms

* Windows
* Linux

---

## Link to the Project Wiki

[See the full project proposal here](https://github.com/Jubiito1/ToTheTop/wiki)

---

## How to Build and Run

### Requirements

* Java Development Kit (JDK) 21
* Git
* A Gradle-compatible IDE such as IntelliJ IDEA

The project includes the Gradle Wrapper, so installing Gradle separately is not required.

### 1. Clone the repository

```bash
git clone https://github.com/Jubiito1/ToTheTop.git
cd ToTheTop
```

### 2. Import the project

Open the project in IntelliJ IDEA and import it as a Gradle project.

Make sure the project is configured to use **Java 21**.

### 3. Run the game

The game can be launched directly from IntelliJ by running:

```text
lwjgl3/src/main/java/com/TfPSR/ToTheTop/lwjgl3/Lwjgl3Launcher.java
```

Alternatively, use the Gradle Wrapper from a terminal.

#### Linux

```bash
./gradlew lwjgl3:run
```

#### Windows

```bash
gradlew.bat lwjgl3:run
```

---

## Project Structure

```text
ToTheTop/
├── core/
│   └── src/main/java/com/TfPSR/ToTheTop/
│       ├── asset/
│       ├── core/
│       ├── entity/
│       ├── input/
│       ├── map/
│       ├── menus/
│       └── physics/
│
├── lwjgl3/
│   └── src/main/java/com/TfPSR/ToTheTop/lwjgl3/
│
├── assets/
│   ├── maps/
│   ├── sprites/
│   └── ...
│
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
└── README.md
```
