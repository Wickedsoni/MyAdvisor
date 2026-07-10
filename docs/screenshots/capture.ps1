# MyAdvisor — visual QA screenshot capture (E5)
#
# Captures every screen in empty + populated states across light and dark themes
# into this folder. Doubles as the Play-listing asset source (roadmap task B5).
#
# ASSUMPTIONS
#   - A single emulator/device is attached (AVD Pixel_8a, 1080x2400, 3-button/gesture
#     nav). The tap COORDINATES below are for that resolution — re-measure for others.
#   - The debug APK is already built and installed:
#       .\gradlew.bat :androidApp:assembleDebug
#       adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk
#   - The bundled dataset is the 3-card seed (Axis ACE, HDFC Regalia Gold, HDFC Swiggy).
#
# NOTE ON AUTOMATION
#   Theme is toggled with `adb shell cmd uimode night <yes|no>`; the running Activity
#   does NOT pick up the config change, so we force-stop + relaunch between passes.
#   The merchant flow types into the search field via `input text`, so an IME must be
#   present (default emulator IME is fine). Coordinate taps are inherently brittle — if
#   a shot lands on the wrong state, re-run that block. Screens are captured with
#   `screencap` to a device temp file, then pulled (never pipe binary through PowerShell
#   redirection — it corrupts PNGs via UTF-16 re-encoding).

$ErrorActionPreference = "Stop"
$sdk = "C:\Users\Avik\AppData\Local\Android\Sdk"
$adb = "$sdk\platform-tools\adb.exe"
$pkg = "com.wickedcoder.myadvisor"
$act = "$pkg/.MainActivity"
$out = $PSScriptRoot

# --- Pixel_8a (1080x2400) coordinates ---------------------------------------
$navRecommend = @(172, 2250)
$navCatalog   = @(540, 2250)
$navMyCards   = @(908, 2250)
$searchField  = @(540, 580)
$firstSuggest = @(720, 754)   # first merchant suggestion row ("Swiggy")
$amountField  = @(540, 615)
$recommendBtn = @(540, 820)
$addColumn    = 898           # x of the Add / Remove trailing button
$rowY         = @(475, 796, 1136)  # y of catalog rows 1..3

function Tap($xy) { & $adb shell input tap $xy[0] $xy[1] }
function Grab($name) {
    & $adb shell screencap -p /data/local/tmp/s.png
    & $adb pull /data/local/tmp/s.png "$out\$name.png" | Out-Null
    Write-Host "captured $name"
}
function Set-Theme($mode) {  # "no" = light, "yes" = dark
    & $adb shell "cmd uimode night $mode" | Out-Null
    & $adb shell am force-stop $pkg
    Start-Sleep -Milliseconds 500
    & $adb shell am start -n $act | Out-Null
    # wait for the Activity to resume, then let first-frame settle
    do { Start-Sleep -Milliseconds 800 } until (
        (& $adb shell dumpsys activity activities) -match "ResumedActivity.*$pkg"
    )
    Start-Sleep -Milliseconds 1200
}
function Do-SwiggyFlow {
    Tap $navRecommend;  Start-Sleep -Milliseconds 600
    Tap $searchField;   Start-Sleep -Milliseconds 400
    & $adb shell input text "Swiggy"; Start-Sleep -Milliseconds 500
    Tap $firstSuggest;  Start-Sleep -Milliseconds 500
    Tap $amountField;   Start-Sleep -Milliseconds 400
    & $adb shell input text "20000"; Start-Sleep -Milliseconds 300
    & $adb shell input keyevent 4    # dismiss IME
    Start-Sleep -Milliseconds 300
    Tap $recommendBtn;  Start-Sleep -Milliseconds 1500
}

# --- Reset to a clean, deterministic state ----------------------------------
& $adb shell pm clear $pkg | Out-Null   # fresh import, 0 owned cards

# === LIGHT PASS =============================================================
Set-Theme "no"
Grab "recommend_empty_light"
Tap $navMyCards; Start-Sleep -Milliseconds 600
Grab "mycards_empty_light"
Tap $navCatalog; Start-Sleep -Milliseconds 600
Grab "catalog_light"                     # unadded (Add buttons)
foreach ($y in $rowY) { & $adb shell input tap $addColumn $y; Start-Sleep -Milliseconds 400 }
Grab "catalog_added_light"               # "Added" morph
Tap $navMyCards; Start-Sleep -Milliseconds 600
Grab "mycards_populated_light"
Do-SwiggyFlow
Grab "recommend_results_light"

# === DARK PASS ==============================================================
# Cards added above persist across the theme restart (user zone survives).
Set-Theme "yes"
Grab "recommend_empty_dark"
Tap $navMyCards; Start-Sleep -Milliseconds 600
Grab "mycards_populated_dark"
Do-SwiggyFlow
Grab "recommend_results_dark"
Tap $navCatalog; Start-Sleep -Milliseconds 600
Grab "catalog_added_dark"                # "Added" morph
# Remove all to reach the empty/unadded states (tap the top Remove as the list reflows)
Tap $navMyCards; Start-Sleep -Milliseconds 700
1..3 | ForEach-Object { & $adb shell input tap 865 497; Start-Sleep -Milliseconds 600 }
Grab "mycards_empty_dark"
Tap $navCatalog; Start-Sleep -Milliseconds 600
Grab "catalog_dark"                      # unadded (Add buttons)

Set-Theme "no"                           # leave the device in light mode
Write-Host "Done. 12 screenshots in $out"
