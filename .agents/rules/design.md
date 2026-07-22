---
trigger: always_on
---

# MathyMove - Game Design, Visuals and Aesthetics

This document serves as the comprehensive design specification and aesthetic rulebook for **MathyMove**. It details the visual language, design assets, typography, color palettes, micro-animations, component measurements, border specifications, transparency mechanics, and layer hierarchy across the application.

---

## 1. Aesthetic Philosophy & Visual Identity

**MathyMove** adheres to a **Monochromatic Greyscale Minimalist Design**. The visual philosophy prioritizes clarity, focus, and elegance, providing a calm and intellectually stimulating environment free from color distractions.

### Core Visual Principles
- **Monochromatic Precision**: High-contrast greyscale tones ranging from off-white (`#F2F2F4`) to charcoal black (`#2C2C2E`).
- **Geometric Simplicity**: Crisp circular nodes, thin connecting vector lines, and smoothly curved container cards (`24.dp` corner radius).
- **Subtle Motion & Tactility**: Spring-driven camera transitions, distance-based node fading, remainder break-off animations, and pulsing target indicators.
- **Structured Typography**: Light weight numbers for quiet elegance paired with bold accents for active focus and hierarchy.

---

## 2. Master Color Palette (`Color.kt` & `Theme.kt`)

The color system relies strictly on hex tokens defined in `Color.kt`:

| Token Name | Hex Code | Color Description | Context & Usage |
| :--- | :--- | :--- | :--- |
| `GreyBackground` | `#F2F2F4` | Light Off-White / Soft Grey | Primary canvas background, screen background |
| `GreySurface` | `#FFFFFF` | Pure White | Surface cards, dialog containers, HUD primary text |
| `GreySurfaceVariant` | `#E5E5EA` | Light Grey Surface | Subtle background surface variants |
| `GreyBorder` | `#D1D1D6` | Mid Light Grey | Divider lines, dialog borders, custom scrollbar thumb |
| `TextPrimary` | `#1C1C1E` | Deep Charcoal Black | Headings, primary titles, unvisited node labels |
| `TextSecondary` | `#636366` | Slate Grey | Subtitles, stat section labels, active connecting lines |
| `TextDisabled` | `#A1A1A6` | Muted Grey | Inactive elements, disabled state text |
| `NodeActiveBackground` | `#2C2C2E` | Charcoal Black | Currently active node fill, top HUD container background, primary button fill |
| `NodeActiveText` | `#F2F2F4` | Off-White / Soft Grey | Text & operator symbols inside the active node circle |
| `NodeNormalBackground` | `#E5E5EA` | Light Soft Grey | Unvisited node circle background |
| `NodeNormalText` | `#1C1C1E` | Deep Charcoal Black | Text & operator symbols inside unvisited nodes |
| `NodeVisitedBackground` | `#D1D1D6` | Mid Light Grey | Visited node circle background |
| `NodeVisitedText` | `#8E8E93` | Muted Slate Grey | Text & operator symbols inside visited nodes |
| `LineColor` | `#C7C7CC` | Soft Light Grey | Interconnecting vector lines between standard nodes |
| `LineActiveColor` | `#636366` | Dark Slate Grey | Vector lines connected to `activeNodeId`, active node outer stroke ring |

---

## 3. Node Circle Assets & Component Specs

The central visual asset of MathyMove is the **Node Circle**, which displays numerical values or mathematical operators (`+`, `-`, `x`, `÷`).

### Geometry & Touch Boundaries
- **Circle Radius (`circleRadiusPx`)**: `84.0f` px (168 px diameter in Canvas render units).
- **Interactive Tap Hitbox**: `circleRadiusPx + 18.0f` (102 px radius) to ensure responsive touch accessibility.
- **Node Spacing**: Center-to-center distance between adjacent connected nodes is `346.0f` px.

### Node Visual States & Styles

| State | Fill Color | Outer Border Ring | Text Color | Font Size & Weight |
| :--- | :--- | :--- | :--- | :--- |
| **Active Node** | `NodeActiveBackground` (`#2C2C2E`) | `LineActiveColor` (`#636366`), stroke radius `91.2f` px (`radius + 7.2f`), width `4.8f` px | `NodeActiveText` (`#F2F2F4`) | `28.8 sp`, `FontWeight.Bold` |
| **Unvisited Node** | `NodeNormalBackground` (`#E5E5EA`) | None (Solid fill) | `NodeNormalText` (`#1C1C1E`) | `28.8 sp`, `FontWeight.Medium` |
| **Visited Node** | `NodeVisitedBackground` (`#D1D1D6`) | None (Solid fill) | `NodeVisitedText` (`#8E8E93`) | `28.8 sp`, `FontWeight.Medium` |

---

## 4. Transparency, Distance Fading & Vector Lines

### Distance-Based Alpha Hierarchy (BFS Graph Traversal)
Opacity levels dynamically fade based on Breadth-First Search (BFS) graph distance from the `activeNodeId`:

| Graph Distance | Opacity Level | Transparency % | Visual Effect & Purpose |
| :--- | :--- | :--- | :--- |
| **Distance 0** (Active Node) | `1.0f` (100%) | 0% | Fully opaque, crisp focus. Unaffected by distance fading. |
| **Distance 1** (Direct Children / Parent) | `0.7f` (70%) | 30% | Clearly visible adjacent choice nodes. |
| **Distance 2** (2 steps away) | `0.4f` (40%) | 60% | Softly visible upcoming tree paths. |
| **Distance \(\ge 3\)** (Pruned/Far) | `0.0f` (0%) | 100% | Fades out completely via smooth `1500ms` linear transition. |

### Connecting Line Vectors
- **Standard Branch Lines**:
  - Color: `LineColor` (`#C7C7CC`) with `alpha = minOf(parentAlpha, childAlpha)`.
  - Stroke Width: `4.8f` px.
- **Active Connected Lines** (Connecting directly to `activeNodeId`):
  - Color: `LineActiveColor` (`#636366`) at `1.0f` alpha.
  - Stroke Width: `7.2f` px (`4.8f * 1.5f`).

---

## 5. Motion, Micro-Animations & Visual Effects

### Smooth Camera Panning
- **Behavior**: The viewport automatically centers on the `activeNodeId` \((x, y)\).
- **Animation Spec**: `spring(dampingRatio = LowBouncy, stiffness = Low)` for a smooth, natural camera drift across the infinite canvas.

### Dropped Remainder Break-Off Animation
- **Trigger**: When division leaves a fractional remainder (e.g., dividing 7 by 2 produces integer `3` with dropped remainder `"0.5"`).
- **Duration**: `3000ms` (3 seconds) using `LinearEasing`.
- **Visual Transformation**:
  - **Position Drift**: Translates `+40px` right and `+20px` down.
  - **Rotation**: Rotates `90°` clockwise (`rotation = progress * 90f`).
  - **Scale**: Shrinks linearly from `1.0x` down to `0.0x`.
  - **Alpha**: Fades linearly from `1.0` to `0.0`.
  - **Typography**: `26.4 sp`, `FontWeight.Bold`.

### Target Number Pulse Transition
- **Trigger**: Target number update when a target is successfully completed.
- **Phase 1 (Fade Out)**: Fades current target to `0.0` alpha over `500ms` (`LinearEasing`).
- **Phase 2 (Pop & Scale Down)**: Target updates, scale snaps to `200%` (`2.0f`) and alpha to `0.0`, then scales down to `100%` (`1.0f`) over `1000ms` (`FastOutSlowInEasing`) while fading in to `1.0` alpha over `1000ms` (`LinearEasing`).

### Low Remaining Moves Warning Pulse
- **Trigger**: `movesRemaining <= 5` and moves count changes.
- **Animation**: Identical 2-phase pulse effect (`500ms` fade out \(\rightarrow\) `1000ms` scale down from 200% to 100% and fade in).

---

## 6. Layouts & UI Component Specifications

### 1. Edge-to-Edge Top HUD Header Banner
- **Container**: Full-width top box with `NodeActiveBackground` (`#2C2C2E`), bottom rounded corners (`bottomStart = 24.dp, bottomEnd = 24.dp`), status bar padding, and `padding(vertical = 16.dp, horizontal = 12.dp)`.
- **Header Label**: `"TARGET"`, `13.2 sp`, `FontWeight.Bold`, `TextSecondary` (`#636366`), letter spacing `2.sp`.
- **Target Display**: Large numeral, `52.8 sp`, `FontWeight.Light`, `GreySurface` (`#FFFFFF`).
- **Stats Row**: 3 equal-width columns (Remaining Moves, Moves Taken, Score).
  - Stat Label: Uppercase, `11.4 sp`, `FontWeight.SemiBold`, `TextSecondary`, letter spacing `0.5 sp`.
  - Stat Value: `20.4 sp`, `FontWeight.Bold`, `GreySurface`.
- **Hamburger Menu Icon**: Top-right corner in HUD, `43.dp` button, `24.dp` canvas drawing 3 horizontal lines (`3.dp` stroke width, color `GreySurface`).
- **Dropdown Menu**: Container `GreySurface`, text `TextPrimary`, `16.8 sp`, `FontWeight.Medium`.

### 2. Start Screen Layout
- **Container**: Centered box on `GreyBackground` (`#F2F2F4`) with `32.dp` padding.
- **Game Title**: `"MathyMove"`, `50.4 sp`, `FontWeight.Light`, `TextPrimary`, letter spacing `4.sp`.
- **Subtitle**: `"Keep your mind active through endless numerical pathways"`, `16.8 sp`, `TextSecondary`, centered alignment.
- **Action Buttons**: Height `56.dp` (primary/outlined) / `52.dp` (secondary), shape `RoundedCornerShape(24.dp)`.
  - Primary Button (Continue/New Game): Container `NodeActiveBackground`, content `GreyBackground`, text `21.6 sp`, `FontWeight.Medium`.
  - Secondary Outlined Button: Border/Text `TextPrimary` or `TextSecondary`, text `21.6 sp` / `19.2 sp`.

### 3. Dialogs & Overlays
- **Game Over ("Try again") Dialog**:
  - Container: `GreySurface` (`#FFFFFF`), title `28.8 sp`, `FontWeight.Bold`, `TextPrimary`.
  - Body Text: `18 sp` (`TextSecondary`) & `19.2 sp` (`TextPrimary`).
  - Action Button: Height `48.dp`, shape `RoundedCornerShape(18.dp)`, container `NodeActiveBackground`, content `GreyBackground`, text `19.2 sp`, `FontWeight.Medium`.
- **High Scores Dialog**:
  - Container: `GreySurface`, title `26.4 sp`, `FontWeight.Bold`, `TextPrimary`.
  - Score Entry Row: Rank `#N` (`18 sp`, `FontWeight.Bold`, `TextSecondary`), Score (`19.2 sp`, `FontWeight.Bold`, `TextPrimary`), Date (`14.4 sp`, `TextSecondary`).
  - Dividers: `HorizontalDivider` in `GreyBorder` at `0.5f` alpha.
  - Custom Scrollbar: Height `24.dp`, width `4.8.dp`, shape `RoundedCornerShape(2.4.dp)`, color `GreyBorder` (`#D1D1D6`).

---

## 7. Layering Architecture (Z-Index Hierarchy)

To maintain visually clean UI boundaries, components are layered in the following order:

```
[Layer 5: Modal Dialogs & System Overlays]   (Game Over, High Scores, Dropdown Menu)
  └── [Layer 4: Fixed Top HUD Header]         (Edge-to-edge dark container & status stats)
        └── [Layer 3: Visual Effect Overlays]  (Dropped Remainder animation Toast)
              └── [Layer 2: Node Circles & Labels] (Solid node fills, outer rings, text)
                    └── [Layer 1: Interconnecting Lines] (Vector line graph connections)
                          └── [Layer 0: Infinite Canvas] (GreyBackground #F2F2F4 surface)
```
