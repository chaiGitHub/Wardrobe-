# High-Density UI, Scrcpy Mode Parity & Webapp ZIP Restore

Upgrade the Android wardrobe app to eliminate excess button/text white space with compact 4–8dp density, deliver seamless tablet and scrcpy desktop mode parity via top header tabs and responsive multi-column grids, and enable full webapp `backup.zip` restore with drilldown presets.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed in Phase 1:
> - **Spacing & White Space Density**: Adopt webapp compact density (4 to 8dp padding, reduced vertical margins, tight button heights) across all screens to eliminate empty white space and maximize information visibility.
> - **Webapp ZIP Restore Mode**: Perform a complete overwrite restore from webapp `backup.zip` archives, replacing clothes, wear sessions, laundry status, photos, and drilldown presets (`custom_presets_json_v3` in SharedPreferences and internal storage).
> - **Scrcpy & Wide Display Layout**: Render top navigation header tabs on wide displays (>600dp / landscape / desktop scrcpy) while hiding bottom navigation, paired with dynamic 2-to-4 column responsive grid layouts for cards and subgroup folders.

---

### 1. Overview & Core Concept

- **What It Does**: Optimizes the wardrobe and wear journal app for both mobile and high-resolution desktop/tablet scrcpy mirroring. It eliminates oversized paddings, fixes button spacing, provides webapp-identical top tab switching, and guarantees full backward-and-forward backup/restore compatibility with the webapp's `backup.zip` format.
- **Target Persona**: Users managing garments, laundry hygiene, wear journals, and deep drilldown reports seamlessly between mobile phones, tablets, and computer scrcpy windows.
- **Key Value**: Crisp information density without bloated empty gaps, true desktop scrcpy window ergonomics, and zero data loss when migrating or restoring backups created in the webapp.

---

### 2. User Experience & Visual Design

#### Layout Density & White Space Elimination
- **Compact Button Padding**: Standardize button content padding to `PaddingValues(horizontal = 12.dp, vertical = 6.dp)` with min-height ~36–40dp instead of bloated 56dp+ slots, while retaining accessible touch bounds.
- **Card Margins & Text Spacing**: Reduce outer card margins to 6–8dp, vertical text gutters to 2–4dp, and icon-to-text spacers to 4–6dp.
- **Dense Grid Spacing**: Change grid arrangements from 16dp spacing down to 8dp horizontal / 8dp vertical spacing, mirroring the webapp's Tailwind `gap-2` and `gap-3` rules.

#### Scrcpy & Wide Display Mode
- **Adaptive Top Header**: When screen width is 600dp or wider (or landscape window in scrcpy), `AppHeader` displays horizontal navigation pill buttons:
  `[ 👕 Closet ] [ 💧 Laundry ] [ 📖 Journal ] [ 🌳 Reports ] [ ✨ AI Advisor ]`
- **Bottom Navigation Behavior**: Hidden automatically on wide displays so no vertical height is squandered.
- **Responsive Multi-Column Grids**:
  - `ClosetScreen`: 1 column on narrow phones, 2 columns on compact tablets (~600dp), 3 columns on medium tablets (~840dp), 4 columns on large monitors / scrcpy windows (>1100dp).
  - `DrilldownScreen`: Subgroup folders render as responsive multi-column cards rather than full-width stretched blocks.
  - `LaundryScreen`: Hamper cards and laundry stats roll into responsive 2-column or 3-column rows.
  - `JournalScreen`: Wear session cards display in responsive 2-column grids on wide displays.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Drilldown Presets Persistence & ZIP Embedding**
  - *Chosen Approach*: Store and read custom presets directly from SharedPreferences (`wardrobe_drilldown_presets_v3` key `custom_presets_json_v3`), and simultaneously serialize them into both `database.json` (`drilldownPresets` field) and separate ZIP entries (`drilldown_presets.json` and `presets.json`).
  - *Why*: Ensures complete compatibility whether restoring an Android backup, webapp backup, or custom preset export.
  - *Alternative Considered*: Storing presets only in internal files, which failed to sync with the active UI `ReportPresetService`.
- **Decision 2: Adaptive Scrcpy Header vs Side Navigation Rail**
  - *Chosen Approach*: Top header pill tabs directly within the brand header bar.
  - *Why*: Perfectly matches the layout of https://github.com/2017xlee-svg/warddrobe in desktop/laptop browser mode, offering familiarity and maximizing vertical layout consistency.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        App Navigation Shell                            │
│  Wide / Scrcpy: Top Tab Pills (Closet, Laundry, Journal, Drill, AI)    │
│  Phone: Bottom Navigation Bar (Hidden on wide / scrcpy)                │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
       ┌───────────────────────────┼───────────────────────────┐
       ▼                           ▼                           ▼
┌───────────────┐           ┌───────────────┐           ┌───────────────┐
│ Closet Screen │           │ Laundry Care  │           │   Drilldown   │
│ Compact Grids │           │ Compact Cards │           │ Subgroup Grid │
└───────┬───────┘           └───────┬───────┘           └───────┬───────┘
        │                           │                           │
        └───────────────────────────┼───────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     WardrobeRepository & Room DB                       │
│  - backup.zip Ingestion (database.json / initialdata / root clothes)   │
│  - SharedPreferences Sync (custom_presets_json_v3)                     │
│  - Photo Extraction & Linking (internal filesDir/photos/)              │
└────────────────────────────────────────────────────────────────────────┘
```

#### ZIP Restore Parsing Strategy
1. **Entry Recognition**:
   - `database.json`, `initialdata.json`, `initial_seed.json`, or root JSON array containing garment records.
   - `drilldown_presets.json`, `presets.json`, or `report_presets.json`.
   - Photo folders: `photos/`, `garments/`, `reports/`, `uploads/`, `table/`, or root image files (`.jpg`, `.jpeg`, `.png`, `.webp`).
2. **Preset Extraction & Activation**:
   - Parse preset array from ZIP entry or `rootObj.optJSONArray("drilldownPresets")`.
   - Write serialized JSON into SharedPreferences `wardrobe_drilldown_presets_v3` under `custom_presets_json_v3`.
   - Also write to `filesDir/drilldown_presets.json` as a durable backup.
3. **Full Overwrite Execution**:
   - Clear existing DB tables, insert all garments, wear sessions, laundry status, and persist extracted photos.

---

## Verification Plan

1. **Compilation Check**: Run `compile_applet` to confirm 100% build pass with no syntax or type errors.
2. **Whitespace & Density Inspection**: Confirm compact 4–8dp margins and reduced button padding in `ClosetScreen`, `DrilldownScreen`, `LaundryScreen`, `JournalScreen`, and `AppNavigation`.
3. **Scrcpy / Wide Mode Inspection**: Verify top header tabs and multi-column grid rendering on wide screen configurations (>600dp).
4. **ZIP Restore Test**: Confirm that loading a webapp `backup.zip` correctly restores all clothes, wearing reports, photos, and drilldown presets.
