---
name: Serene Empathy System
colors:
  surface: '#f8fafb'
  surface-dim: '#d8dadb'
  surface-bright: '#f8fafb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f5'
  surface-container: '#eceeef'
  surface-container-high: '#e6e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#3f484b'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#eff1f2'
  outline: '#6f797c'
  outline-variant: '#bec8cb'
  surface-tint: '#086878'
  primary: '#006474'
  on-primary: '#ffffff'
  primary-container: '#2d7d8e'
  on-primary-container: '#f3fcff'
  inverse-primary: '#87d2e4'
  secondary: '#006876'
  on-secondary: '#ffffff'
  secondary-container: '#9eefff'
  on-secondary-container: '#046f7d'
  tertiary: '#545c5e'
  on-tertiary: '#ffffff'
  tertiary-container: '#6c7576'
  on-tertiary-container: '#f3fcfd'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#a9edff'
  primary-fixed-dim: '#87d2e4'
  on-primary-fixed: '#001f26'
  on-primary-fixed-variant: '#004e5b'
  secondary-fixed: '#9eefff'
  secondary-fixed-dim: '#82d3e2'
  on-secondary-fixed: '#001f24'
  on-secondary-fixed-variant: '#004e59'
  tertiary-fixed: '#dbe4e5'
  tertiary-fixed-dim: '#bfc8c9'
  on-tertiary-fixed: '#151d1e'
  on-tertiary-fixed-variant: '#404849'
  background: '#f8fafb'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
typography:
  display-lg:
    fontFamily: Manrope
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Manrope
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Manrope
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-md:
    fontFamily: Manrope
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  container-margin: 24px
  gutter: 20px
  stack-sm: 12px
  stack-md: 24px
  stack-lg: 40px
---

## Brand & Style

The brand personality is rooted in **Empathetic Professionalism**. Moving away from the current dark, "gamer-centric" aesthetic, this design system embraces a **Modern Clinical** style—clean, structured, and dependable, yet softened by human-centric details. 

The UI should evoke a sense of safety and calm, critical for a simulator dealing with sensitive psychological topics like family violence. It utilizes a mix of **Minimalism** and **Soft Tonal Layering** to create a focused, low-stress environment for students and educators.

**Target Audience:** Psychology students, clinical supervisors, and academic researchers.
**Emotional Response:** Trust, clarity, emotional safety, and intellectual focus.

## Colors

The palette is designed to be "clinical yet human." It replaces the previous high-contrast dark mode with a soothing, accessible light-themed foundation.

- **Primary (Soft Teal):** Used for primary actions and navigation headers. It conveys professional authority without the aggression of pure blue.
- **Secondary (Supportive Blue):** Used for active states and highlights, providing a gentle visual anchor.
- **Backgrounds (Warm Grays):** The base surfaces use slightly warm grays to avoid the sterile coldness of pure white, making long reading sessions more comfortable.
- **Functional Colors:** Success and Error states are muted to maintain a calm atmosphere while remaining clearly communicative for test results and feedback.

## Typography

Typography focuses on maximum legibility and a balanced academic tone.

- **Manrope** is used for headings to provide a modern, friendly, and geometric structure that feels organized and reliable.
- **Inter** is used for all body text and data labels. Its high x-height ensures clarity in technical descriptions and case studies.
- **Hierarchy:** We use generous line heights (1.5x for body text) to assist in reading complex psychological case narratives. Bold weights are reserved for critical status indicators and section titles.

## Layout & Spacing

The layout follows a **Fluid Grid** system within a max-width container (1280px) to ensure the interface feels expansive and uncrowded.

- **Desktop:** 12-column grid with 24px margins. Content is organized into "Cards" that act as cognitive containers for information.
- **Tablet:** 8-column grid with 20px margins. Sidebars reflow into top-level navigation or hidden drawers.
- **Mobile:** 4-column grid with 16px margins. Cards stack vertically to maintain readability of long case texts.
- **Rhythm:** An 8px base unit drives all padding and margin decisions, creating a predictable and harmonious vertical rhythm.

## Elevation & Depth

To maintain a "clinical yet human" feel, we avoid heavy shadows. Instead, we use **Tonal Layers** and **Low-Contrast Outlines**.

- **Surface 0 (Background):** Neutral Gray-100.
- **Surface 1 (Cards):** White, with a subtle 1px border (#E2E8F0).
- **Surface 2 (Interactive):** Very soft ambient shadows (4px blur, 5% opacity) are used only when an element is hovered or active to signify "tactility."
- **Depth:** Content hierarchy is created through background color shifts (e.g., a slightly darker gray for the sidebar) rather than dramatic shadow stacking.

## Shapes

The shape language is **Softly Geometric**. 

A corner radius of **8px (0.5rem)** is the standard for most components, providing a modern look that feels approachable but maintains professional structure. Larger cards or "Hero" sections (like the Login container) use **16px (1rem)** to emphasize their role as primary interaction hubs. Buttons utilize a medium roundness to feel friendly but distinct from playful "pill" shapes.

## Components

### Buttons
- **Primary:** Solid Soft Teal with white text. Focus on clear, high-contrast labels.
- **Secondary:** Outlined teal or gray for less urgent actions like "View Details."
- **Ghost:** Used for navigation or utility actions to reduce visual noise.

### Cards
- Case cards should feature a clear header, a brief summary, and "tags" for case categories (e.g., "Family Violence").
- Use a white background with a subtle border to separate case content from the page background.

### Input Fields
- Labels are always visible above the field in `label-md`.
- Borders are light gray, turning primary teal on focus. 
- Avoid dark "gamer" backgrounds; use white or very light gray fields for maximum contrast.

### Chips & Badges
- Used for "Status" (e.g., Active, Blocked, Passed).
- Status badges use low-saturation background tints of success/error colors with darker text to ensure accessibility without being jarring.

### Dashboard Stats
- Stat blocks (Total Students, etc.) should use a simplified version of the primary card, with large, clear `display-lg` numbers for quick scanning.