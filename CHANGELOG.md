# Changelog

All notable changes to Katapult are documented here.

## [1.5]

### Added
- **Lockscreen Music Controls** (Kompakt only): the stock MuditaOS lockscreen media widget only works with Mudita's own player, so anything else playing left the lock screen with no controls. Katapult now draws its own music widget in the same spot for those players, with the same outline as the notification widget. Off by default, needs the Katapult Action Service, and stays hidden for the Mudita player so the two never appear together. The home screen widget still works with every player, Mudita's included.
- **Search Settings** in the System section: opens Android's settings search so you can find a system setting by name without digging through menus.
- **Icon Size** in Appearance: switch app icons between 72 x 72 and 80 x 80 on both the home screen and All Apps. Row and column spacing stays the same, so only the icons grow.
- **Show Weather** (Kompakt only): Shows emperature above the clock, beside the alarm and battery, on the home screen and the screensaver. Syncs with the weather app on the device.
- **Show Alarm** toggle: hide the next-alarm row above the clock.

### Changed
- New installs from 1.5 onward start with the extra dock row on, vertical All Apps gestures, page arrows hidden, infinite scroll off, and 80 x 80 icons. Updating from an earlier version changes nothing: your settings are kept exactly as they are, icons included.
- The home dock rows now sit on the same grid All Apps uses, so the rows line up when switching between the two screens. Holds whether app names and the status bar are shown or hidden, and the All Apps button lines up with the app icons beside it.
- The album art placeholder in the music widget is now a black note on white instead of white on black, matching the rest of the widget.
- Hide App Names moved from the Home Screen section to Appearance, since it affects labels on both the home screen and All Apps.

### Fixed
- Czech, Dutch, French, German, Italian, Polish, Portuguese, and Spanish were missing 19 strings, including the whole Check for Updates flow, which rendered in English. All nine languages are complete again.

## [1.4]

### Added
- **App Shortcuts**: long-press an app in All Apps and pick App Shortcuts to enable the shortcuts it publishes (New timer, a specific conversation, and so on). Enabled shortcuts appear in All Apps as their own tiles with reorder, rename, custom icons, and remove, and can be assigned to any home slot including clock and date. Requires Katapult to be the default launcher.
- Pinned shortcut support: "Add to Home screen" from the browser (websites and PWAs) and pin requests from other apps now land in All Apps instead of going nowhere.
- **Config Export/Import**: save all settings to a timestamped JSON file and restore them later, plus a Clear All Data option. Imports are validated, so a foreign or corrupted file is rejected instead of wiping settings.
- **Check for Updates**: in-app updater that checks GitHub releases, shows the release notes, and downloads and installs the new APK.
- **Double Tap in home** now cycles between Disabled, Brightness, and Lock Phone (locking uses the accessibility service). Replaces the Double-Tap Brightness toggle; an enabled toggle carries over as Brightness.
- **Hide Status Bar Clock** setting (Kompakt only): covers the duplicate status bar clock while the launcher is in the foreground, since the home screen shows its own clock. Needs the accessibility service; off by default.
- **Material Icons** in Change Icon: the dialog now has two tabs, Katapult Icons (bundled set plus PNG/SVG import, as before) and Material Icons with 133 outlined Material icons to assign to any app.
- **Rejected calls and voicemail in the Phone badge** (Kompakt only): the badge now counts declined calls and waiting voicemail alongside missed calls, mirroring how the Mudita phone app and launcher behave on MuditaOS 1.6.0, so both launchers show the same number.
- **Clear Notifications for the Phone app** (Kompakt only): long-press Phone in All Apps to reset its badge. The Phone and Messages badges are read from the call log and SMS databases rather than notifications, so this option was previously hidden. It now works for Phone, and also clears counts the stock launcher cannot.

### Changed
- The accessibility service is now listed as **Katapult Action Service** in system accessibility settings, since it powers the lockscreen widget, the status bar clock cover, phone locking, and the screensaver. Only the label changed; an already enabled service stays enabled after updating.
- Tapping the Katapult icon in All Apps (Show Katapult Icon) now opens Katapult Settings while Katapult is the default launcher, instead of just returning to the home screen. When another launcher is default, the icon opens the app normally.

### Fixed
- AOSP Calendar, WebView DevTools, AOSP Search (Quick Search Box), and Chrome now get bundled default icons instead of their stock ones.
- Turning on Lockscreen Notifications or Screensaver on power no longer saves the setting when the Katapult Action Service was never enabled. The toggle used to stick on while the feature did nothing, and enabling the service later for an unrelated reason would switch it on unannounced.

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
