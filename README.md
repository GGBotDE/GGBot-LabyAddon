# GGBot LabyMod Addon

The official LabyMod 4 addon for [GGBot](https://www.ggbot.de). It brings GGBot
into Minecraft, so you can shop at bots, manage your own bots, and keep an eye on
them without leaving the game.

GGBot is a Minecraft bot hosting service. It runs your bots 24/7 on its own
infrastructure, so they stay online even when your computer is off. Bots are
configured from the web panel (or from this addon) and can run a range of modules,
including shop, kick areas, ticket handling, and custom logic built with the
block based plugin editor.

This addon works for two kinds of players:

- People who just want to **buy from GGBots** on a server. No GGBot account needed.
- **Bot owners** who want to manage and watch their bots from inside Minecraft.

---

## Table of contents

- [Features](#features)
- [Supported Minecraft versions](#supported-minecraft-versions)
- [Installing](#installing)
- [First time setup](#first-time-setup)
- [Default hotkeys and commands](#default-hotkeys-and-commands)
- [Settings](#settings)
- [Building from source](#building-from-source)
- [Project layout](#project-layout)
- [How it works](#how-it-works)
- [Contributing](#contributing)
- [License](#license)
- [Links](#links)

---

## Features

### For everyone (no account required)

- Walk up to a GGBot, look at it, and open its shop with a hotkey. The shop GUI
  lets you browse and buy items from bots that run the Sell Module.
- A hint appears near an online GGBot telling you the shop is available and which
  key opens it.
- `Check GGBot` entry in the player interaction menu to confirm whether a nearby
  player is an online GGBot.
- A command that lists the GGBots currently on the server, with a button to copy
  a bot name to the clipboard. The list is searchable and can be filtered to
  online bots.
- An optional tab list indicator that shows how many GGBots are online on the
  current server.

### For bot owners (GGBot account linked)

- Link your GGBot account through LabyMod and pick which bot you are working with.
- A bot management menu with tabs for overview, modules, logs, tools, and tickets.
- Start and stop bots from the menu or the bot selector.
- View live status, statistics, and logs. Logs refresh right after you send a
  command, both from the in game menu and the chat prefix.
- Send commands directly to your bot from the menu, or from chat using a
  configurable prefix.
- View and edit module locations (Sell area, sell drop location, buy chest
  override, GGFeatures trash chest) as editable block coordinates, with a
  from-look button, save, and an in-world marker toggle.
- See the whole Sell area drawn as a box in the world.
- Highlight kick areas in the world.
- Show the contents of a buy or sell chest when you look at it, and search saved
  shop items and mark the chest that holds a match.
- WorldEdit style position selection that sends the position commands to your bot.
- Control mode: take over a nearby online bot, see from its head, and steer it
  with your movement keys and mouse.
- Manage Ticket Module tickets in a clean GUI: list open and closed tickets,
  read the conversation, reply, and close a ticket.
- Player interactions for `Follow Player` and `Attack Player` that only appear
  when the matching module is enabled on your bot.
- HUD widgets for bot name, status, balance, health, citybuild, plot, and ticket
  counts.

New features and integrations are added over time.

---

## Supported Minecraft versions

The addon targets LabyMod 4 and is built for:

1.8.9, 1.12.2, 1.16.5, 1.17.1, 1.18.2, 1.19.4, 1.20.1, 1.20.4, 1.20.6, 1.21,
1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2.

The full feature set, including the world overlays and the in game text, is
verified across this range. Note that the overlay render path differs on newer
releases (see [How it works](#how-it-works)).

---

## Installing

For players, install it like any other LabyMod addon, through the LabyMod
addon store inside the launcher or the in game addon menu. There is nothing to
configure to start using the shop; account linking is only needed for the bot
owner features.

---

## First time setup

The first time you join a server with the addon installed, a short setup guide
opens. It asks whether you want to shop or whether you own a bot, and then walks
you through the relevant steps and hotkeys. You can cancel it at any time and
reopen it later from the addon settings under General Settings.

Shop users do not need to do anything else. Bot owners link their account from
the settings (`Auth GGBot Account`) and then pick a bot under `Available Bots`.

---

## Default hotkeys and commands

All keys can be changed in the settings.

| Action | Default |
| --- | --- |
| Open shop of the bot you look at | Ctrl + O |
| Open the bot selector | Ctrl + B |
| Open the bot management menu | Ctrl + G |
| Toggle control mode | Ctrl + K |
| Set WorldEdit position 1 | Numpad 1 |
| Set WorldEdit position 2 | Numpad 2 |

Commands:

- `/ggbots` (alias `/serverbots`) opens the list of GGBots on the current server.

Hotkeys do not fire while chat or any menu is open.

---

## Settings

The settings are grouped into sections:

- **Bot**: the selected bot and account linking (Auth, Discord, start, stop).
- **Bot Commands**: the chat prefix used to send commands to your bot.
- **Bot Logs**: how often logs are fetched.
- **Shop**: enable the shop, the shop key, and the bot detection range.
- **Status Updates** and **Statistics Updates**: how often the HUD widgets refresh.
- **General Settings**: the Check GGBot interaction, join notification, bot
  selector shortcut, follow and attack player interactions, the tab list bot
  count and its horizontal offset, and the backup overlay render engine.
- **Bot Management Menu**: the menu and position hotkeys, kick area highlighting,
  and chest content display.
- **World Overlays**: colors for each overlay type, outline thickness, fill
  toggle and opacity, the maximum render distance, and the chest content position.

---

## Building from source

Requirements:

- JDK 21
- The bundled Gradle wrapper

Common tasks:

```
./gradlew build
```

This builds the addon for every configured Minecraft version. The output jars are
placed under the build directories of the modules.

To run a development client, use the run task that LabyGradle generates for your
target version. List the available tasks with:

```
./gradlew tasks
```

and run the client task for the version you want to test on.

The set of target versions lives in `gradle.properties`
(`net.labymod.minecraft-versions`). The GGBot SDK version
(`de.ggbot.sdk-version`) and the fallback addon version
(`de.ggbot.addon-version`) are defined there as well. The release version can be
overridden with the `VERSION` environment variable.

---

## Project layout

This is a standard LabyMod 4 multi module addon.

- `api` - the addon API reference module used by LabyMod tooling.
- `core` - all of the addon logic. This is where almost everything lives. The
  entry point is `de.ggbot.core.GGBot`. Notable packages:
  - `api` - calls to the GGBot backend through the SDK, plus the versioning and
    feature flag handler.
  - `auth` - the OAuth flow for linking a GGBot account.
  - `cfg` - the configuration classes and their settings.
  - `commands` - chat commands such as `/ggbots`.
  - `gui` - the screens: `botmenu`, `botselector`, `onboarding`, `serverbots`,
    and `shop`.
  - `interactions` - player interaction menu entries (Check GGBot, follow, attack).
  - `listener` - hotkeys, the chat command prefix, server join and disconnect,
    and timers.
  - `nametag` - bot name tag rendering.
  - `overlay` - the world and HUD overlay rendering (boxes, lines, labels, chest
    contents, shop hint, tab list count).
  - `widget` - the HUD widgets (info, ingame, ggfeatures, ticket).
  - `utils` - shared helpers, including the key combo trigger and the render
    engine selector.
- `game-runner` - per version modules used to launch and test the addon in a
  development client. There is one source set per supported Minecraft version.

The addon depends on the GGBot SDK (`de.ggbot:ggbot-sdk`) for the backend API.

---

## How it works

A few design points worth knowing if you read or review the code.

**Version independence.** The addon logic is written once in `core` against the
LabyMod 4 API and runs on every supported Minecraft version. Addon features do
not use mixins or version specific Minecraft code. Where private access would be
needed, that is the exception and is documented in place.

**Overlay rendering.** The world overlays project their 3D coordinates to the
screen and draw them as batched 2D shapes, so a marked area or a chest highlight
stays correct at any camera angle and does not need a mixin. There are two draw
back ends: the legacy render pipeline used on 1.8 through 1.21.5, and a canvas
based engine required from 1.21.8 onward, where the legacy pipeline no longer
produces output. The engine is chosen automatically from the running version, and
the choice can be forced from the settings.

**Feature flags and versioning.** Network features are gated behind feature flags
that are resolved from the GGBot backend, and each feature resolves its own base
URL. This means individual features can be enabled, disabled, or pointed at a
different endpoint without shipping a new build, and backend errors are reported
back rather than crashing the client. Unknown flags default to enabled.

**Control mode.** Steering a bot reads your camera look and movement keys, sends
them to the bot, hides the bot model, and moves the view onto the bot. Because the
camera handling differs across Minecraft versions, the implementation picks the
right approach per version (moving the camera entity on older releases, offsetting
the render view on newer ones) so it never desyncs or teleports your own player.

**Localization.** All user facing text goes through the i18n files. English
(`en_us`) and German (`de_de`) are included.

---

## Contributing

Contributions are welcome. To keep the code consistent and reviewable, please
follow the conventions already used in the project:

- Keep features version independent. Avoid mixins and version specific code for
  addon behavior, and avoid reflection.
- Put every user facing string in the i18n files, for both English and German.
- Match the surrounding style and add documentation comments where it helps.
- Avoid changes that hurt performance on the render or tick path. Do not allocate
  per frame what can be cached.
- Do not log with `System.out`; use the addon logger.

Build with `./gradlew build` before opening a pull request and make sure the
addon still starts on at least one target version.

---

## License

This repository does not currently include a license file. All rights are
reserved by GGBot (www.GGBot.de) unless stated otherwise. If you want to reuse
parts of this code, please contact the team first.

---

## Links

- Website: https://www.ggbot.de
- Discord: https://discord.ggbot.de