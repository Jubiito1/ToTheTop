# Changelog

All notable changes to this project are documented in this file.
The format is based on Keep a Changelog, and this project adheres
to Semantic Versioning.

---
## [0.4.0] - 2026-08-31

### Added
- Added a reusable Settings menu accessible from the main menu and pause menu.
- Added music and sound-effect volume controls.
- Added a shared audio manager for global audio settings.
- Added reusable slider and label styles to the UI skin.

### Changed
- Centralized UI style creation in `AssetService`.
- Updated the application window title to `ToTheTop`.
- Renamed screen package to menus.

---

## [0.3.0] - 2026-08-30

### Added
- Added a pause menu, opened with ESC, with Continue, Main Menu, and Settings buttons.

---

## [0.2.0] - 2026-08-28

### Added
- Implemented the complete physical character body using Box2D.
- Added head, torso, arms, forearms, and legs to the player character.
- Added physics joints connecting the character's body parts.
- Added angular limits to the character's joints.
- Added automatic body-part density calculation based on the character's weight.
- Added player arm movement following the mouse cursor.
- Added left and right arm states.
- Added grip point functionality prototype.
- Added final basic game controls.
- Added fullscreen startup and cursor hiding.
- Added `AssetService` based on LibGDX's `AssetManager`.
- Added support for loading textures, sounds, and music through the asset system.
- Added the main menu and loading screen.
- Added Singleplayer, Multiplayer, and Settings menu buttons.
- Added main menu background image.
- Added main menu music.
- Added dash sound effect.
- Added initial screen management system.
- Added initial Tiled map rendering with a test map and rock objects.

### Changed
- Renamed the main Java package from `com.TfPSR.CucoProject` to `com.TfPSR.ToTheTop`.
- Renamed the project package structure accordingly across the `core` and `lwjgl3` modules.
- Reworked the project launcher to use the new `ToTheTop` package.
- Replaced the previous direct game startup flow with a screen-based architecture.
- Reorganized classes into functional packages (`entity`, `input`, `map`, `physics`, `asset`, `screen`).
- Updated the game to use a shared `Main` instance for screen and rendering management.
- Updated the character implementation to support sound effects.
- Updated the asset loading system to use file extensions required by LibGDX loaders.

### Fixed
- Fixed the LWJGL3 launcher referencing the old `CucoProject` package.
- Fixed music loading by specifying the `.ogg` file extension.
- Fixed integration issues between the loading screen, main menu, asset service, and game screen.
- Fixed package references after renaming the project.
- Fixed hand dragging behavior in the character and input handling.

### Removed
- Removed the old grip point system implementation.
- Removed obsolete grab-point naming and references.
- Removed all classes and methods related to the previous movement system.
- Removed obsolete pre-refactor classes left in the root package.

---

## [0.1.0] - 2026-07-17
### Added
- Initial project setup with LibGDX (Liftoff).
- Base module structure: `core` and `lwjgl3`.
- Created the README with the project information.
- Published the project proposal on the Wiki.
- Renamed the project from CucoProject to To The Top.
