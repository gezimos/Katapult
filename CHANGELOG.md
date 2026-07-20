# Changelog

All notable changes to Katapult are documented here.

## [1.3]

### Added
- **Dark Mode** setting — inverts the whole launcher to white-on-black, including app icons (the same monochrome silhouettes, filled white instead of black). Manual toggle; off by default.
- **Text Backgrounds** setting — draws white rounded "islands" (matching the icon corner radius and border) behind the clock, date, status row, and app names so text stays legible over a wallpaper. Off by default.
- **Swipe for All Apps** setting — open All Apps with a swipe on the home screen. Direction follows the gesture setting: swipe up (vertical) or swipe left (horizontal). Off by default.
- **App Gesture Direction** setting — switch All Apps page navigation between Horizontal and Vertical swipes.
- Adaptive All Apps grid — the number of rows now scales to the screen height, so taller devices show more apps per page and shorter devices fewer (instead of a fixed 3×4). With app names hidden, an extra row fits.
- Swipe back to Home from the first All Apps page (when infinite scroll is off) — swipe right (horizontal) or down (vertical).
- Reorder hint tooltip — entering reorder mode shows a floating hint ("Tap an app to swap it with the highlighted one") that dismisses on the first tap or after a few seconds.
- **Experimental: Lockscreen Notifications** — shows per-app notification counts on the lock screen as an island-style widget, via an accessibility overlay (must be enabled in system Accessibility settings). Includes an app allowlist and latest/oldest sort; long-press the widget to move and resize it, tap to save. Off by default. Note: the accessibility permission sounds scary, but Katapult uses it only to draw the widget and to read the lock screen itself — never your apps — so the widget hides when the PIN screen appears; it records and shares nothing, as its open source code proves.
- **Experimental: Screensaver** — a Katapult screen saver (dream) showing the home clock, date, alarm, battery, and up to 4 notification rows pinned to the bottom. Start it with a two-finger long-press on the home screen (or the home long-press menu); the power button exits it. Select Katapult once as the system screen saver — Settings → Experimental → Screensaver opens the system picker. Android also auto-starts it while charging.
- Czech translation, and completed Dutch; all new strings translated across all 9 languages.

### Changed
- Settings reorganized into titled sections: Appearance, Home Screen, All Apps, Gestures, Notifications, E-Ink (Mudita only), and System.
- Renamed the Extra Dock Row description to "Show 3 extra apps in home".

### Fixed
- With the Rounded icon shape, the small icons in the shortcut app picker and Hidden Apps list rendered as circles — the corner radius now scales with icon size.
- With app names hidden, home shortcuts and All Apps icons now render icon-only (no reserved label space), so icon spacing matches between the two screens.

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
