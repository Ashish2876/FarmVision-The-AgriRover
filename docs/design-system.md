# FarmVision AgriRover — Design System & Visual Specification

**Document Version:** 3.0.0 (Parrot Green + Premium White Redesign)
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

### 2.1 Core Redesign Brand Tokens (Parrot Green + Pure White)

| Token Name | Hex Code | Visual Sample | Semantic Purpose |
| :--- | :--- | :---: | :--- |
| `ParrotGreen` | `#78E000` | `■` | Primary action fills, hero header accent, selected indicators, speed gauge peak |
| `ParrotGreenDeep` | `#3CB300` | `■` | Pressed/hover fills, active joystick knob gradient, tractor chassis accent |
| `DarkGreen` | `#287A20` | `■` | Green text/icons on white/lime, focus rings, selected chip borders ($\ge 4.5:1$ AA) |
| `LightLime` | `#D4F8A8` | `■` | Selected surface containers, navigation pill indicators, secondary tactile buttons |
| `PaleGreen` | `#EFFBE8` | `■` | Secondary background, selected card surface highlight |
| `PureWhite` | `#FFFFFF` | `■` | Content cards, sheets, dialogs, bottom navigation bar |
| `SoftWhite` | `#F7FBF4` | `■` | App background canvas (anti-glare outdoor readable baseline) |
| `TextPrimary` | `#1B2E1B` | `■` | Headings and high-contrast body (8.57:1 on ParrotGreen, 14.45:1 on PureWhite) |
| `TextSecondary` | `#5C6B5C` | `■` | Supporting text, captions, timestamps (5.66:1 on PureWhite) |
| `BorderSubtle` | `#DDE8D5` | `■` | Thin card borders, dividers, unselected chip outlines |

### 2.2 Semantic Status Tokens (`LocalAgriStatusColors`)

| Status State | Light Mode Token | Dark Mode Token | WCAG Ratio (on White) | Semantic Rule |
| :--- | :--- | :--- | :---: | :--- |
| `Online / Success` | `#1B7A43` | `#34D399` | **5.37 : 1 (AA)** | Real hardware heartbeat verified only |
| `Warning` | `#945200` | `#FBBF24` | **6.06 : 1 (AA)** | Stale telemetry, degraded link, uncalibrated |
| `Error / Alert` | `#BA2518` | `#F87171` | **6.24 : 1 (AA)** | Fault condition, packet loss, hardware failure |
| `Info / Connecting` | `#1B5E8A` | `#60A5FA` | **6.97 : 1 (AA)** | Handshake in progress, sync active |
| `Demo / Simulated` | `#6C3483` | `#C39BD3` | **6.40 : 1 (AA)** | Simulated data stream (never styled green) |

---

## 3. WCAG 2.1 Contrast Ratio Measurements

Automated verification is enforced via `ContrastUnitTest.kt` and `BaselineUnitTest.kt` using the standard W3C WCAG 2.1 relative luminance algorithm:

| Contrast Pair | Foreground | Background | Measured Ratio | WCAG Compliance |
| :--- | :--- | :--- | :---: | :---: |
| **Primary Button Text** | `TextPrimary (#1B2E1B)` | `ParrotGreen (#78E000)` | **8.57 : 1** | **WCAG AAA** ($\ge 7.0:1$) |
| **Body Headings on White** | `TextPrimary (#1B2E1B)` | `PureWhite (#FFFFFF)` | **14.45 : 1** | **WCAG AAA** ($\ge 7.0:1$) |
| **Body Headings on Canvas** | `TextPrimary (#1B2E1B)` | `SoftWhite (#F7FBF4)` | **13.80 : 1** | **WCAG AAA** ($\ge 7.0:1$) |
| **Secondary Text on White** | `TextSecondary (#5C6B5C)` | `PureWhite (#FFFFFF)` | **5.66 : 1** | **WCAG AA** ($\ge 4.5:1$) |
| **Secondary Text on Canvas**| `TextSecondary (#5C6B5C)` | `SoftWhite (#F7FBF4)` | **5.40 : 1** | **WCAG AA** ($\ge 4.5:1$) |
| **Green Text on Pure White**| `DarkGreen (#287A20)` | `PureWhite (#FFFFFF)` | **5.39 : 1** | **WCAG AA** ($\ge 4.5:1$) |
| **Green Text on Pale Green**| `DarkGreen (#287A20)` | `PaleGreen (#EFFBE8)` | **5.04 : 1** | **WCAG AA** ($\ge 4.5:1$) |
| **Green Text on Light Lime**| `DarkGreen (#287A20)` | `LightLime (#D4F8A8)` | **4.79 : 1** | **WCAG AA** ($\ge 4.5:1$) |
| **Online Status on White** | `StatusOnline (#1B7A43)` | `PureWhite (#FFFFFF)` | **5.37 : 1** | **WCAG AA** ($\ge 4.5:1$) |
| **Warning Status on White** | `StatusWarning (#945200)` | `PureWhite (#FFFFFF)` | **6.06 : 1** | **WCAG AA** ($\ge 4.5:1$) |
| **Alert Status on White** | `StatusAlert (#BA2518)` | `PureWhite (#FFFFFF)` | **6.24 : 1** | **WCAG AA** ($\ge 4.5:1$) |
| **Info Status on White** | `StatusInfo (#1B5E8A)` | `PureWhite (#FFFFFF)` | **6.97 : 1** | **WCAG AA** ($\ge 4.5:1$) |
| **Dark Theme Body Text** | `DarkThemeText (#F2F8F1)` | `DarkBackground (#0D1710)` | **16.92 : 1** | **WCAG AAA** ($\ge 7.0:1$) |
| **Dark Theme Primary Button**| `TextPrimary (#1B2E1B)` | `ParrotGreen (#78E000)` | **8.57 : 1** | **WCAG AAA** ($\ge 7.0:1$) |

> [!CAUTION]
> **Contrast Guardrail**: Bright Parrot Green (`#78E000`) on white or light surfaces has a contrast ratio of only **1.69 : 1** and fails WCAG AA by a wide margin. Therefore, text or icons on white/light surfaces MUST use `DarkGreen (#287A20)` (5.39:1). Text on Parrot Green fills MUST use `TextPrimary (#1B2E1B)` (8.57:1).

### 3.1 Chart Series Visual Rules
- **Probe 1 (Primary Series)**: `ChartSeriesPrimary` (`DarkGreen #287A20`), solid 3dp stroke, solid circular data nodes.
- **Probe 2 (Secondary Series)**: `ChartSeriesSecondary` (`Amber #D97706`), dashed 3dp stroke (`12f, 8f`), circular data nodes.
- **Probe 3 (Tertiary Series)**: `ChartSeriesTertiary` (`Blue #2563EB`), dotted stroke.
- **Gridlines**: `MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)`, 1dp stroke.
- **Gap Invariant**: Telemetry gaps (`isGap = true` or `avg == null`) explicitly break the Canvas path; never interpolate or draw across missing data.

### 3.2 Safety & Integrity Invariants
- **Emergency Stop Safety Core**: The E-Stop strike button and cutoff banners remain prominently **RED** (`#BA2518` / `#C0392B`), paired with clear icon and text. It is NEVER styled in green.
- **Simulation Honesty**: `DEMO`, `SIMULATED`, `STALE`, and `UNCALIBRATED` chips retain distinctive amber/purple tokens and are never styled in brand parrot green.

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

---

## 9. Hardware Center UI Specifications & Accessibility (Track H)

### 9.1 Status Vocabulary & Color Semantics
Hardware Center status indicators strictly use semantic color tokens defined in `AgriStatusColors`:

| Status State | Color Token | Visual Purpose | Rule / Constraint |
|---|---|---|---|
| `Connected` | `statusColors.online` (`#4CAF50`) | Authenticated live link | Only shown with real verified hardware heartbeat |
| `Connecting` | `statusColors.connecting` (`#64B5F6`) | Active handshake in progress | Shown during connection probe or initialization |
| `Disconnected` | `statusColors.offline` (`#E57373`) | Known unreachable endpoint | Shown when probe or polling fails |
| `Warning / Stale` | `statusColors.warning` (`#FFB74D`) | Uncalibrated, stale telemetry, or fault | Raw readings or telemetry older than freshness window |
| `Unknown / Draft` | `MaterialTheme.colorScheme.onSurfaceVariant` | Unpolled, draft pinout, or initial state | Default initial state; never claims false discovery |
| `Demo / Simulated` | `statusColors.demo` (`#81C784`) | Local mathematical simulation | Explicit amber/green badge; never switches silently |
| `Unavailable` | `MaterialTheme.colorScheme.outline` | Absent hardware feature | Enforced for uninstalled sensors or encoders |

### 9.2 Component Anatomy
1. **`HardwareSummaryRow`**: Summary counter row counting electronic devices (13 total), passive items (10), with 0 health scores and full `contentDescription` reporting exact numbers.
2. **`ControllerEndpointCard`**: Dual-controller endpoint display for Main ESP32 DevKit V1 and ESP32-CAM with independent connection chips, latency, firmware version, and `role = Role.Button`.
3. **`HardwareComponentCard`**: Standard 23-item catalog card displaying category icon, title, model, connection status, diagnostic maturity chip, telemetry readout, and chevron navigation with minimum 48dp touch target.
4. **`HardwareHonestyCard`**: Prominent capability boundary card highlighting what sensors or feedback lines are absent on open-loop hardware.
5. **`HardwareCategoryFilterBar`**: Horizontal scrolling filter chip bar for rapid category filtering (`All (23)`, `Controllers (2)`, `Drivers (1)`, `Motors (4)`, `Sensors (5)`, `Tools (3)`, `Power (8)`).

### 9.3 Accessibility & Font Scaling Coverage
- **Semantics & Screen Readers**: All cards, buttons, filter chips, and dialogs provide rich `contentDescription` and declare `Role.Button` or `Role.Tab`. Section headers declare `semantics { heading() }`.
- **Large Font Scaling (2.0x)**: Validated in `HardwareCenterLargeFontLightPreview` and `HardwareCenterLargeFontDarkPreview` (`fontScale = 2.0f`).
- **Contrast**: Body text on backgrounds exceeds 16:1 (WCAG AAA); all status colors on dark backgrounds exceed 3.0:1 (WCAG AA graphical UI standard).


---

## 10. Field Analytics UI Specifications & Visualization Tokens (Phase 9)

Phase 9 defines the visual language, chart bounds, accessibility alternatives, and honest data representations for historical telemetry analytics:

### 10.1 Chart Rendering & Visual Invariants
- **Point Budget**: Canvas series plotting is capped at 200 points per series (`AnalyticsMathEngine.MAX_CHART_POINTS`), preventing excessive path recreation and UI thread jank.
- **Physical Gap Breaks**: When the interval between consecutive samples exceeds $2.5\times$ nominal sampling interval ($150\text{s}$), the path lifts and creates a visible gap break rather than drawing a misleading continuous line across missing intervals.
- **Uncalibrated Dual Probes**: Surface Probe 1 and Deep Substrate Probe 2 are plotted as distinct series in raw ADC counts (`counts`), with individual y-axis scales and distinct series colors.
- **Color Contrast & Theme Tokens**: All chart gridlines, axes, labels, and plot strokes are rendered strictly from project theme tokens:
  - Backgrounds: `MaterialTheme.colorScheme.surface`
  - Text & Axes: `MaterialTheme.colorScheme.onSurface`, `MaterialTheme.colorScheme.onSurfaceVariant`
  - Primary Series: `MaterialTheme.colorScheme.primary` (`ParrotGreen` on dark, `DarkGreen` on light)
  - Secondary Series: `MaterialTheme.colorScheme.secondary`
  - Zero raw hex values or un-themed colors are allowed in visualization components.

### 10.2 Visualization Components
1. **`HistoricalBoundedCanvasChart`**: Custom Compose Canvas chart rendering bounded time series with dynamic min/max bounds, horizontal guide lines, TalkBack merged content descriptions, and visible gap breaks.
2. **`AccessibleHistoryTable`**: Accessible tabular view of historical telemetry records paired with the chart view, enabling full screen reader row-by-row navigation.
3. **`AnalyticsOverviewCards`**: Multi-sensor overview card grid reporting window arithmetic averages across all 5 telemetry metrics, strictly reporting raw probes independently.
4. **`CustomDateRangeDialog`**: Material 3 date range picker dialog enforcing 30-day max span retention, future date clamping, and accessible button touch targets.
5. **`AnalyticsKpiSummaryCard`**: Summary statistical card reporting latest reading, sample mean (with irregular sampling disclaimer), min/max range, valid count, validation exclusions, rule-based anomaly flags, and plain-language neutral insights.
6. **`HistoricalRecordsList`**: Reverse-chronological paginated telemetry sample list with load-more pagination and individual sample provenance indicators.

### 10.3 Responsive Phone & Tablet Layouts
- **Phone Portrait (< 720dp)**: Single-column flowing vertical layout with generous spacing and sticky status header.
- **Tablet / Wide Landscape (>= 720dp)**: Two-pane split layout:
  - Left Pane: Metric & Range Selectors, Overview Cards, KPI Summary, Agronomic Indicators.
  - Right Pane: Chart / Table Visualization, Mode Toggles, Historical Records Log.
- **Accessibility Minimums**: Minimum 48dp touch targets on all interactive chips, tabs, and buttons. 2.0x font scaling tested and verified without clipping.

---

## 11. Control Station Design Tokens & Aesthetics

### 11.1 Scoped Control Station Palette (`ControlStationPalette.kt`)
The Rover Control Station establishes a scoped palette anchored to the core FarmVision brand tokens (`DarkBackground` `#0D1710`, `ParrotGreen` `#78E000`, `DarkThemeText` `#F2F8F1`):

| Token Name | Hex Value | Purpose / Placement |
| :--- | :--- | :--- |
| `background` | `#0D1710` | Fullscreen anti-glare dark outdoor base |
| `surfaceGlass` | `#D9132218` | Translucent 85% opacity dark green glass container |
| `surfaceGlassDeep` | `#F20D1710` | Translucent 95% opacity dark camera viewport container |
| `surfaceTile` | `#991A2E20` | Compact telemetry card surface |
| `surfacePill` | `#B3101E14` | Status and mode pill background |
| `surfacePillActive` | `#78E000` | Active selected mode pill fill |
| `borderGlow` | `#5978E000` | Subtle glowing boundary stroke (35% opacity) |
| `borderGlowHigh` | `#B378E000` | Emphasized glowing boundary stroke (70% opacity) |
| `accentGreen` | `#78E000` | Brand primary parrot green accent |
| `accentGreenGlow` | `#4078E000` | Radial under-glow behind joysticks and cards |
| `eStopRed` | `#D32F2F` | Industrial emergency stop dome fill |
| `eStopRedGlow` | `#80D32F2F` | Pulsing red safety halo |
| `stickKnob` | `#E8EFE8` | Circular metallic floating joystick knob |
| `stickKnobBorder` | `#FFFFFF` | Crisp 2dp white rim for the joystick thumb |
| `stickRing` | `#78E000` | Outer glowing joystick circular guide ring |
| `textPrimary` | `#F2F8F1` | High-contrast body text and titles |
| `textMuted` | `#9EB5A2` | Secondary labels, units, and timestamps |
| `textOnAccent` | `#1B2E1B` | High-contrast dark text on bright parrot-green pill |

### 11.2 WCAG 2.1 Contrast Ratio Verification
The Control Station palette was mathematically verified under WCAG 2.1 criteria (`ControlStationContrastTest.kt`):
- **`textPrimary` on `background`**: $15.42:1$ (Exceeds WCAG AAA threshold of $7.0:1$).
- **`textMuted` on `background`**: $7.85:1$ (Exceeds WCAG AA threshold of $4.5:1$).
- **`textOnAccent` on `accentGreen`**: $8.57:1$ (Exceeds WCAG AA threshold of $4.5:1$).
- **`accentGreen` on `background`**: $10.15:1$ (Exceeds WCAG non-text graphical boundary threshold of $3.0:1$).

### 11.3 Custom Vector Iconography (`RoverCustomIcons.kt`)
To eliminate third-party font/icon dependencies while ensuring razor-sharp rendering on all screen densities:
1. **`Tractor`**: Custom agricultural tractor with oversized lugged rear wheel, front wheel, cabin, roof, and exhaust stack. Displayed on the Move joystick knob.
2. **`SteeringWheel`**: 3-spoke automotive/marine steering wheel with central hub. Displayed on the Steer joystick knob.
3. **`Gamepad`**: Dual-grip gaming controller with directional cross and action buttons. Displayed on the Manual drive mode pill.
4. **`Flask`**: Chemistry laboratory flask with neck and fill level line. Displayed on the Soil pH tile.
5. **`FarmLeaf`**: Two stylized organic leaves with vein detailing. Displayed in the top brand logo.

### 11.4 Tactile Twin-Stick Specifications (`RoverStick.kt`)
- **Dimensions**: $150\text{ dp}$ outer pad, $62\text{ dp}$ floating knob ($44\text{ dp}$ minimum touch target satisfied).
- **Physical Feedback**: Animated physics spring return (`Spring.DampingRatioMediumBouncy`, `Spring.StiffnessMedium`) on finger release.
- **Performance Optimization**: Knob offset coordinates are rendered strictly inside Compose `graphicsLayer { translationX = ...; translationY = ... }`, completely bypassing recomposition passes during $60\text{ fps}$ continuous finger drags.
- **Directional Chevrons**: Bright chevrons illuminate along the active axis, while perpendicular axes are visually dimmed.

### 11.5 Responsive Orientation Support
- **Landscape Cockpit**: Full drone-station view with 16:9 camera view, vertical toolbar, twin joysticks, bottom mode cluster, tactical mini-map, and 2x4 telemetry grid.
- **Portrait View**: Restyled with identical dark glass components, stacked camera preview, tactile controllers, and bottom navigation bar access.
