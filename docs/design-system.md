# FarmVision AgriRover — Design System & Visual Specification

**Document Version:** 2.0.0 (Phase 2 Baseline)  
**System Name:** AgriVision Design System  
**Framework:** Jetpack Compose (Material 3)  
**Package:** `com.farmvision.agrirover.core.designsystem`  

---

## 1. Visual Philosophy & Design Principles

The AgriVision design system combines precision agricultural robotics engineering with organic nature-inspired design. It provides a consistent, accessible, outdoor-readable interface across small phone screens and rugged outdoor tablets.

```
       [PRECISION ROBOTICS]                  [ORGANIC NATURE]
      Clean geometric borders                Leaf-inspired hues
     Telemetry status gauges                 Bio-sensing indicators
     Tactile control surfaces               Natural ambient contrast
                  \                              /
                   \                            /
                    [FARMVISION VISUAL IDENTITY]
                     • Futuristic yet pragmatic
                     • Ultra-readable outdoors
                     • Zero distracting clutter
```

### Core Design Pillars
1. **Futuristic Yet Pragmatic**: Restrained visual hierarchy, clean surfaces, and subtle borders. Avoids excessive neon glows or low-contrast decorative glassmorphism.
2. **Outdoor & High-Sunlight Readability**: Designed for harsh sunlight and fluctuating outdoor lighting conditions. High contrast ratios (exceeding WCAG AAA for body text) ensure effortless readability without eye strain.
3. **Ergonomic Field Usability**: Sized for operators wearing field gloves or managing tasks with one hand. All interactive touch targets meet or exceed $48 \times 48\text{dp}$.
4. **Tokenized Consistency**: Colors, dimensions, typography scales, and shapes are strictly centralized in `core/designsystem/theme/`. Zero hardcoded hex values, raw dp numbers, or scattered shape literals.

---

## 2. Color Palette & Semantic Tokens

### 2.1 Core Brand Tokens

| Token Name | Hex Code | Visual Sample | Semantic Purpose |
| :--- | :--- | :---: | :--- |
| `DeepForest` | `#123B2A` | `■` | Primary brand anchor, dark container backgrounds, brand hero cards |
| `PrimaryGreen` | `#218C5B` | `■` | Interactive triggers, verified indicators, dark theme primary highlights |
| `LightPrimaryGreen`| `#176840` | `■` | Light theme primary container, tuned for 6.34:1 contrast on WarmOffWhite |
| `FreshLeaf` | `#62C985` | `■` | Dark theme primary container, active readings, 9.1:1 contrast on DarkBackground |
| `MintAccent` | `#A8E6C1` | `■` | Fine borders, glowing radar indicators, tactical badge text |
| `WarmOffWhite` | `#F5F8F3` | `■` | Light theme background, neutral high-contrast canvas |
| `DarkBackground` | `#0B1410` | `■` | Deep obsidian backdrop, maximizes outdoor OLED contrast and battery life |
| `DarkSurface` | `#14221B` | `■` | Elevated cards, dialog surfaces, control module backdrops |
| `DarkSurfaceVariant`| `#1C2E25` | `■` | Secondary cards, subtle nested containers, disabled chip bases |
| `DarkSurfaceHigh` | `#243B30` | `■` | High-emphasis cards, header bars, floating toolbars |

---

## 3. WCAG 2.1 Contrast Ratio Measurements

Automated verification was conducted using the W3C WCAG 2.1 relative luminance calculation algorithm (implemented in `ContrastCalculator.kt` and asserted in `ContrastUnitTest.kt`).

| Contrast Pair | Foreground | Background | Measured Ratio | WCAG Compliance |
| :--- | :--- | :--- | :---: | :---: |
| **Dark Theme Body Text** | `DarkThemeText (#E8F1EB)` | `DarkBackground (#0B1410)` | **16.24 : 1** | **WCAG AAA** ($\ge 7.0:1$) |
| **Dark Theme Card Body** | `DarkThemeText (#E8F1EB)` | `DarkSurface (#14221B)` | **14.29 : 1** | **WCAG AAA** ($\ge 7.0:1$) |
| **Dark Theme Muted Text** | `TextMutedDark (#9EB4A7)` | `DarkBackground (#0B1410)` | **8.50 : 1** | **WCAG AAA** ($\ge 7.0:1$) |
| **Dark Theme Primary Button**| `DarkBackground (#0B1410)` | `FreshLeaf (#62C985)` | **9.10 : 1** | **WCAG AAA** ($\ge 7.0:1$) |
| **Dark Theme Online Status** | `StatusOnline (#2ECC71)` | `DarkBackground (#0B1410)` | **8.91 : 1** | **WCAG AAA** ($\ge 7.0:1$) |
| **Light Theme Body Text** | `PrimaryTextLight (#17231C)`| `WarmOffWhite (#F5F8F3)` | **15.15 : 1** | **WCAG AAA** ($\ge 7.0:1$) |
| **Light Theme Card Body** | `PrimaryTextLight (#17231C)`| `LightSurface (#FFFFFF)` | **16.23 : 1** | **WCAG AAA** ($\ge 7.0:1$) |
| **Light Theme Muted Text**| `TextMutedLight (#43584C)` | `WarmOffWhite (#F5F8F3)` | **7.16 : 1** | **WCAG AAA** ($\ge 7.0:1$) |
| **Light Theme Primary Button**| `WarmOffWhite (#F5F8F3)` | `LightPrimaryGreen (#176840)`| **6.34 : 1** | **WCAG AA** ($\ge 4.5:1$) |
| **Light Theme Online Status**| `LightAgriStatus.online (#1B7A43)`| `WarmOffWhite (#F5F8F3)` | **5.01 : 1** | **WCAG AA** ($\ge 4.5:1$) |

---

## 4. Spacing, Shapes, and Touch Targets

All dimensions adhere to `FarmVisionSpacing`:
- **4dp Modular Scale**:
  - `xxSmall`: 2dp (micro offsets)
  - `extraSmall`: 4dp (internal badge padding, icon-text spacing)
  - `small`: 8dp (compact list margins, chip paddings)
  - `medium`: 12dp (card content gaps, button vertical padding)
  - `large`: 16dp (standard screen and card padding)
  - `extraLarge`: 24dp (section separation)
  - `xxLarge`: 32dp (major module separation)
  - `huge`: 48dp (minimum accessible touch target)
- **Accessible Touch Target**: Guaranteed $\ge 48\text{dp}$ height and width for all interactive components (`FarmVisionPrimaryButton`, `FarmVisionIconButton`, etc.).
- **Material 3 Shapes**:
  - `Shapes.extraSmall`: 6dp (micro tags, status chips)
  - `Shapes.small`: 10dp (buttons, icon containers)
  - `Shapes.medium`: 16dp (standard telemetry cards, dialogs)
  - `Shapes.large`: 22dp (hero banners, bottom sheets)

---

## 5. Typography Scale & Tabular Figures

In addition to the standard Material 3 typographic scale, the design system defines specialized numeric styles with **tabular figures (`tnum`)** so rapidly changing sensor telemetry values (speed, battery, soil moisture) do not cause horizontal layout jitter:

```kotlin
val TelemetryValueLarge = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    fontFeatureSettings = "tnum"
)

val TelemetryValueMedium = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    fontFeatureSettings = "tnum"
)
```

---

## 6. Component Inventory & Usage Examples

Located in `com.farmvision.agrirover.core.designsystem.component`:

### 6.1 Buttons (`FarmVisionButton.kt`)
Interactive buttons with guaranteed 48dp minimum touch targets, optional leading icons, and loading spinner states:
```kotlin
// Primary Action
FarmVisionPrimaryButton(
    text = "Start Mission",
    onClick = { /* ... */ },
    leadingIcon = Icons.Rounded.PlayArrow,
    isLoading = false
)

// Secondary Tonal Action
FarmVisionSecondaryButton(
    text = "Configure Sensors",
    onClick = { /* ... */ }
)

// Outlined Action
FarmVisionOutlinedButton(
    text = "Calibrate RTK",
    onClick = { /* ... */ }
)

// Text Auxiliary Action
FarmVisionTextButton(
    text = "View History",
    onClick = { /* ... */ }
)

// Icon Button (Guaranteed 48dp touch target)
FarmVisionIconButton(
    icon = Icons.Rounded.Settings,
    contentDescription = "Open Settings",
    onClick = { /* ... */ }
)
```

### 6.2 Modular Cards (`FarmVisionCard.kt`)
Base surface container composed of dedicated header, body, and footer slots:
```kotlin
FarmVisionCard {
    FarmVisionCardHeader(
        title = "Rootzone Moisture",
        supportingText = "Probe array 0x48",
        leadingIcon = Icons.Rounded.WaterDrop,
        trailingAction = { FarmVisionStatusBadge(status = FarmVisionStatusType.Active) }
    )
    Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
    Text("Active capacitive readings nominal.")
    Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
    FarmVisionCardFooter(
        statusContent = { Text("Sample Data", color = MaterialTheme.colorScheme.primary) },
        actionContent = { FarmVisionTextButton(text = "Details", onClick = {}) }
    )
}
```

### 6.3 Status Indicators (`FarmVisionStatus.kt`)
Accessible status badges covering all 8 operational and development states:
```kotlin
FarmVisionStatusBadge(status = FarmVisionStatusType.Online)
FarmVisionStatusBadge(status = FarmVisionStatusType.Offline)
FarmVisionStatusBadge(status = FarmVisionStatusType.Connecting)
FarmVisionStatusBadge(status = FarmVisionStatusType.Active)
FarmVisionStatusBadge(status = FarmVisionStatusType.Idle)
FarmVisionStatusBadge(status = FarmVisionStatusType.Warning)
FarmVisionStatusBadge(status = FarmVisionStatusType.Error)
FarmVisionStatusBadge(status = FarmVisionStatusType.DemoData)
```
- **Rule**: Never conveys status by color alone; always pairs a distinct icon with text.
- **Rule**: Status components must **never** default to or imply "Online" or "Connected". Simulated data must be marked with `DemoData`.

### 6.4 Sensor Metrics (`FarmVisionMetric.kt`)
Reusable metric cards for agricultural readings with tabular number support:
```kotlin
FarmVisionMetricCard(
    label = FarmVisionMetricType.SoilMoisture.defaultLabel,
    value = "42.8",
    unit = FarmVisionMetricType.SoilMoisture.defaultUnit,
    icon = FarmVisionMetricType.SoilMoisture.icon,
    trend = FarmVisionTrendDirection.Rising,
    trendLabel = "+1.4%",
    isSampleData = true
)
```

### 6.5 Section Headers (`FarmVisionSectionHeader.kt`)
Clean group headers with optional supporting text and trailing action triggers:
```kotlin
FarmVisionSectionHeader(
    title = "Field Telemetry",
    description = "Real-time crop sensors",
    leadingIcon = Icons.Rounded.Sensors,
    trailingAction = { FarmVisionTextButton(text = "Export", onClick = {}) }
)
```

### 6.6 State Components (`FarmVisionState.kt`)
Standard state containers communicating system conditions with actionable recovery paths:
```kotlin
// Empty state
FarmVisionEmptyState(
    title = "No Missions Found",
    message = "Create your first scan mission to catalog crop data.",
    actionText = "New Mission",
    onActionClick = { /* ... */ }
)

// Loading state
FarmVisionLoadingState(message = "Calibrating rover compass...")

// Recoverable error state
FarmVisionErrorState(
    title = "Telemetry Lost",
    message = "No heartbeat packet received from ESP32 for 500ms.",
    onRetry = { /* ... */ }
)

// Unavailable hardware state
FarmVisionUnavailableState(
    title = "Soil Probe Offline",
    message = "Check hardware cable connection."
)

// Simulation / Demo Mode banner
FarmVisionDemoBanner(message = "SIMULATION ACTIVE: Telemetry is simulated. Rover hardware not connected.")
```

### 6.7 Small Helpers (`FarmVisionHelpers.kt`)
```kotlin
// Reusable icon container
FarmVisionIconContainer(
    icon = Icons.Rounded.Sensors,
    contentDescription = null
)

// Standard horizontal divider
FarmVisionDivider()
```

---

## 7. Accessibility Standards & Verification

1. **Touch Targets**: All interactive elements (`FarmVisionPrimaryButton`, `FarmVisionIconButton`, clickable cards) enforce `Modifier.defaultMinSize(48.dp, 48.dp)`.
2. **Semantics & Roles**:
   - Buttons declare `Role.Button`.
   - Status indicators expose `contentDescription = "Status: $displayLabel"`.
   - Informative icons have meaningful descriptions; decorative icons use `null`.
3. **Contrast Ratio Compliance**:
   - High-contrast text exceeds WCAG AAA ($\ge 7.0:1$) in both light and dark themes.
   - Interactive button text meets WCAG AA ($\ge 4.5:1$).
4. **Large Font Scale (2.0x)**:
   - Verified via `@Preview(fontScale = 2.0f)` in `FarmVisionPreviews.kt` to ensure no text clipping or overlapping elements.
5. **Narrow Screens (320dp)**:
   - Verified via `@Preview(widthDp = 320)` in `FarmVisionPreviews.kt` ensuring responsiveness on compact phone screens.

---

## 8. Viewing Previews in Android Studio

All component previews are collected in:
`com.farmvision.agrirover.core.designsystem.component.FarmVisionPreviews.kt`

Previews include:
- `PreviewButtonsDark` & `PreviewButtonsLight`
- `PreviewButtonsLargeFontScale` (`fontScale = 2.0f`)
- `PreviewStatusBadges` (All 8 StatusType variants)
- `PreviewCardsDark`
- `PreviewMetricsNarrow` (`widthDp = 320`)
- `PreviewStates` (Empty, Loading, Error, Unavailable, Demo Banner)
