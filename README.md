# Skycast 🌤️

A free, ad-free, privacy-friendly weather app for Android — live conditions, 24-hour and 10-day forecasts, air quality, and multiple saved cities. Built with Jetpack Compose, Clean Architecture, MVI and a fully modularized Gradle build.

## Features

- **Live weather**: current conditions, feels-like temperature, wind, humidity, UV, pressure, visibility, sunrise and sunset, and air quality (US AQI)
- **Forecasts**: the next 24 hours and the next 10 days
- **Multiple cities**: swipe between saved places; search worldwide; use your current location
- **Always fresh**: auto-refreshes while open, refreshes when you come back online, and syncs in the background with WorkManager
- **Offline-first**: the last forecast stays available with no connection
- **Instant settings**: units (°C/°F, km/h/mph/m/s/kn, mm/in, hPa/inHg), 12/24h clock, light/dark/system theme, dynamic color
- **English and Arabic**, with full RTL support
- **Free**: data comes from [Open-Meteo](https://open-meteo.com) — no API key, no ads, no tracking

## Architecture

Clean Architecture, split into Gradle modules along layer and feature boundaries:

```
app                    → DI graph, navigation, theme wiring
feature/
  home                 → weather dashboard (MVI)
  places               → search, saved cities, current location (MVI)
  settings             → units, theme, about (MVI)
core/
  model        (JVM)   → domain entities, unit conversions
  common       (JVM)   → Outcome, errors, dispatcher qualifiers, ticker
  domain       (JVM)   → repository contracts + use cases
  data                 → offline-first repository implementations
  network              → Open-Meteo Retrofit client
  database             → Room (single source of truth)
  datastore            → user preferences
  location             → LocationManager and Geocoder (no Play Services)
  mvi                  → MviViewModel<State, Intent, Effect> base
  designsystem         → theme, tokens, components, animated backdrops
  ui                   → shared weather UI, formatters, UiText
  testing      (JVM)   → fakes, test data, MainDispatcherRule
sync/work              → periodic background refresh
build-logic            → Gradle convention plugins
```

**Dependency rule:** `feature → domain ← data`. Features never depend on each other or on `data`. The `domain` module is pure Kotlin.

**Data flow (MVI):**

```
UI ──Intent──▶ ViewModel ──UseCase──▶ Repository ──▶ Room ◀── Network
 ▲                 │                                  │
 └──── State ◀─────┴──────────── Flow ◀───────────────┘
        Effect (one-shot: navigation, snackbar, permission)
```

Room is the single source of truth. The UI observes the database, and network refreshes only write to it, so every update reaches the screen as it happens.

## Tech stack

Kotlin 2.2 · Jetpack Compose (Material 3) · Hilt · Coroutines/Flow · Room · DataStore · Retrofit + kotlinx.serialization · WorkManager · Navigation Compose (type-safe) · JUnit, Turbine, Truth

## Getting started

Requirements: Android Studio (latest stable) and JDK 17+.

```bash
git clone https://github.com/mahmoudibrahimabdulfattah/WeatherAppModularized.git
./gradlew assembleDebug
./gradlew testDebugUnitTest test
```

You don't need an API key.

## Attribution

Weather data by [Open-Meteo.com](https://open-meteo.com/) (CC BY 4.0).
