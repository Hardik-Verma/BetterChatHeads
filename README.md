# Better Chat Heads

Better Chat Heads is a lightweight client-side Fabric mod that renders clean player skin heads beside player names in chat and in the tab list while staying safe for vanilla, premium, offline-mode, and cracked servers.

## Features

- Chat heads beside player chat messages when the sender can be linked safely to a real player.
- Tab list heads with a safe fallback for cases where vanilla does not render one.
- Correct modern skin face rendering using both the base face layer and the optional hat or hair overlay layer.
- Steve or Alex style fallback head when a usable skin is missing or unavailable.
- Lightweight JSON config at `config/betterchatheads.json`.
- In-game config screen with live saving via the default `B` key.
- Optional Mod Menu integration when Mod Menu is installed.
- Background version check with a one-time-per-launch update prompt.
- Client-side only and server-safe.

## Supported Version

- Minecraft `1.21.11`

## Configuration

```json
{
  "enableChatHeads": true,
  "enableTabHeads": true,
  "renderOverlayLayer": true,
  "headSize": 8,
  "chatHeadPadding": 2,
  "tabHeadPadding": 2,
  "useDefaultHeadWhenMissingSkin": true,
  "showOwnChatHead": true,
  "chatHeadVerticalOffset": 0,
  "showKeybindActionBarFeedback": true,
  "enableUpdateChecker": true,
  "allowUpdatePromptInGame": false
}
```

You can also edit the same settings in game. Press `B` by default to open the Better Chat Heads config screen, then save to apply changes immediately without restarting the client.

## Keybinds

- `B` opens the Better Chat Heads config screen by default.
- Extra keybinds are included for toggling chat heads, tab heads, overlay rendering, and cycling head size.
- The extra toggle keybinds are unbound by default so players can assign them in Controls without conflicts.

## Mod Menu

- Mod Menu is optional.
- If a player has Mod Menu installed, Better Chat Heads exposes its config screen there automatically.
- If Mod Menu is not installed, the mod still works normally with the built-in config screen.

## Update Checker

- On launch, Better Chat Heads can check for a newer published version in the background.
- If a newer version is found, the mod shows a prompt once per game launch with `Yes` to open the update page or `No` to dismiss it until the next launch.
- By default, the prompt waits for a menu screen instead of interrupting active gameplay. This can be changed in the in-game config.

## Notes

- The mod is client-side only. Servers do not need to install anything.
- Offline-mode and cracked players may not expose a usable premium skin texture, so Better Chat Heads falls back to a default Steve or Alex style head when needed.
- Chat heads are only shown for messages that can be linked safely to a player. System messages, announcements, and untrusted text stay headless.

## Planned

- Broader `1.21.x` coverage after the first `1.21.11` target is stable.
- Future work for the `26.1` lineup once the rendering hooks are validated there.
