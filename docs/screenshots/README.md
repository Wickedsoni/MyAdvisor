# Visual QA screenshots

Reference screenshots of every MyAdvisor screen across **empty + populated** states and
**light + dark** themes. Produced by task **E5** (roadmap Track E). This set doubles as
the source for the Play Store listing assets (task **B5**).

Captured on the `Pixel_8a` emulator (1080×2400) running the debug APK built from `main`,
with the bundled 3-card seed dataset (**Reward DB v1.0.0**: Axis ACE, HDFC Regalia Gold,
HDFC Swiggy Card).

## The set

| Screen | State | Light | Dark |
|---|---|---|---|
| Recommend | empty (search + categories) | `recommend_empty_light.png` | `recommend_empty_dark.png` |
| Recommend | results (Swiggy + ₹20,000) | `recommend_results_light.png` | `recommend_results_dark.png` |
| Catalog | unadded (Add buttons) | `catalog_light.png` | `catalog_dark.png` |
| Catalog | added (✓ Added morph) | `catalog_added_light.png` | `catalog_added_dark.png` |
| My Cards | empty (EmptyState + CTA) | `mycards_empty_light.png` | `mycards_empty_dark.png` |
| My Cards | populated (Remove buttons) | `mycards_populated_light.png` | `mycards_populated_dark.png` |

The **Recommend / results** shot is the canonical hero: #1 = HDFC Swiggy Card **7.5%**
with the "★ BEST PICK" tile, the "≈ ₹1,500 back" value line, and the ₹1,500-cap caveat
banner rendered; #2 = Axis ACE **2.5%** under "Other cards".

## Regenerating

```powershell
# from repo root, with the emulator up and the debug APK installed
.\gradlew.bat :androidApp:assembleDebug
adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk
.\docs\screenshots\capture.ps1
```

`capture.ps1` is the exact script used. It `pm clear`s to a clean state, walks the
light pass, then force-stops + relaunches to apply dark mode (a running Activity does
not pick up `cmd uimode night` on its own) and walks the dark pass. Tap coordinates are
Pixel_8a-specific — re-measure for other resolutions. Screens are pulled via
`screencap` + `adb pull` (never redirect binary through PowerShell `>` — it corrupts
the PNG).

## Notes

- The header shows **Reward DB v1.0.0** because that is the dataset bundled on `main`.
  (The roadmap changelog's C2/C3 entries describe a v1.2.0 / HDFC Millennia dataset that
  was never merged to `main`.) Regenerate this set after that data lands to refresh the
  card counts and version string.
- Accessibility (E4) is not visible in static screenshots; the TalkBack sanity pass is
  tracked separately.
