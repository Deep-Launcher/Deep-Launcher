# Deep Launcher

[![Discord](https://img.shields.io/badge/Discord-Join%20the%20server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/KDh9sFTThC)

A clean, minimal Minecraft launcher built with Kotlin and JavaFX.

Deep Launcher manages multiple game instances with isolated mods, saves and
resource packs, downloads any official Mojang version, handles the Java
runtime and libraries for you, and lets you play with offline accounts you
create and switch inside the launcher.

## Requirements

- JDK 21+

## Build & Run

```sh
./gradlew run
```

The launcher downloads and manages its own per-version Java runtimes, so no
additional setup is needed besides a JDK to build and launch the app.

## Usage

1. Click **+** to create a new instance.
2. Enter a name and pick a Minecraft version from the list.
3. Wait for the download to finish. You can close the *New Instance* dialog and track progress from the **Downloads** button on the sidebar while it runs.
4. Open the **Account** button on the sidebar, type a nickname and press **Add Account**. An offline UUID is generated from your nickname automatically and the account becomes active.
5. Hit **Play** with an account active; the launcher hides while the game runs and reappears when you close it.

## Tech Stack

- **Kotlin** with coroutines
- **JavaFX** for the UI
- **Ktor** for HTTP
- **kotlinx.serialization** for JSON
- **Gradle** for build

## Project Layout

```
app/src/main/kotlin/org/deeplauncher/
├── account/    Offline account store and selection
├── core/       Launcher paths and endpoints
├── network/    Downloads and progress tracking
├── version/    Version manifest and asset handling
├── instance/   Instance repository, manager and game launch
├── runtime/    Java runtime download and extraction
├── models/     Data models
└── ui/         JavaFX UI controller
```

## Contributing

Contributions are welcome and simple:

1. Create a branch from `main`.
2. Make your changes and commit them.
3. Push the branch and open a pull request.
4. Reviewers review your PR, leave feedback if needed, and merge it once it's approved.

Please use [conventional commits](https://www.conventionalcommits.org/) for your commit messages.

## License

[GPL-3.0](LICENSE)