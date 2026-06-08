# Changelog

All notable changes to Katapult are documented here.

## [1.2]

### Added
- **Clock Format** setting — cycle the home clock between System, `20:24` (24-hour), `08:24 PM`, `8:24 PM`, and `8:24`. Replaces the old Show AM/PM toggle; the AM/PM suffix now appears automatically for the 12-hour formats that include it.
- **Date Format** setting — cycle the home date between System (locale long date), `Mon, Jun 8`, `Monday, June 8`, `June 8, 2026`, `6/8/2026`, and `2026-06-08`.
- **Disable Music Widget** setting — hides the home-screen music widget.
- Bundled icons for Discord and Libro.fm.

### Fixed
- The Home button now dismisses any open bottom sheet (menu, app picker, hidden apps, app context menu, rename, change icon, reset confirm) — previously only Back closed them.

## [1.1]

### Added
- Custom icon import — long-press an app → **Change Icon** to pick a bundled Katapult icon or import your own PNG/SVG. SVGs are rasterized and centered at 60% of the icon shape with black fill.
- **Clear Notifications** option in the app context menu — dismisses tray entries for that package so the badge resets. Hidden for Mudita direct-badge packages (call log / SMS). Respects re-posting apps.
- **Hide Arrow Buttons** setting — hides the page arrows and dots in All Apps so the grid fills the full height; swipe to change pages. Reorder controls and the indicator row still appear when relevant.
- **Disable Home Editing** setting — locks home shortcuts so long-press no longer opens the app picker. Instead it opens Android's App Info screen for the assigned app. The empty-area long-press menu (Settings / Hidden Apps / Wallpaper) stays accessible.
- **Hide All Apps Button** setting — replaces the dashed-dot All Apps tile in the dock with a regular shortcut (defaults to the system contacts app). Tap launches it; long-press opens the picker (or App Info when home editing is locked). Drawer access moves to a new **All Apps** entry in the empty-area long-press menu.
- Tap the battery percentage on the home screen to open Android's battery settings.
- App version is now shown at the bottom of the Settings screen.

### Changed
- Monochrome app icons are now always used when the app provides one (API 33+ natively, API 31–32 via drawable XML parsing).
- Notification counting rewritten from a counter to a set of active notification keys — fixes apps that update one notification N times being counted as N separate notifications (e.g. podcast apps, multi-message conversations).
- Translations updated for new strings across de / pt / it / fr / pl / es / nl.

### Fixed
- Re-importing an icon for the same package now correctly invalidates the cache.
- Notification counts stay consistent when a conversation with multiple messages is opened — the badge now clears completely instead of decrementing by one.

## [1.0]

Initial public release.
