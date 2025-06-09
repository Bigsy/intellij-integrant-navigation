# Integrant Navigator

https://plugins.jetbrains.com/plugin/27456-integrant-navigator

An IntelliJ IDEA plugin that provides navigation from namespaced keywords in EDN files (like `config.edn`) to their corresponding Integrant component implementations in Clojure code.

## Demo

[![Integrant Navigator Demo](https://img.youtube.com/vi/XzrysD5HkWQ/0.jpg)](https://www.youtube.com/watch?v=XzrysD5HkWQ)

## Features

- **Keyword Navigation**: Cmd+click (or Ctrl+click on Windows/Linux) on a namespaced keyword in an EDN file to navigate to its corresponding Integrant component implementation.
- **Reference Navigation**: Navigate from `#ig/ref` references to their target configuration.
- **Composite Key Support**: Navigate from composite keys like `#ig/ref [:foo :bar]` to their definitions.
- **Reverse Lookup**: Cmd+click (or Ctrl+click) on a keyword within an Integrant implementation (`defmethod integrant.core/init-key`) to navigate back to its configuration in EDN files.

## Requirements

- IntelliJ IDEA 2024.1 or newer
- Cursive plugin 2025.1.1-241 or compatible version

## Usage

1. Open an EDN file containing Integrant configuration.
2. Hold Cmd (macOS) or Ctrl (Windows/Linux) and click on a namespaced keyword.
3. If a corresponding Integrant component implementation exists (defined with `integrant.core/init-key`), you'll be navigated to it.

### Reverse Lookup

1. Open a Clojure file containing Integrant component implementations.
2. Hold Cmd (macOS) or Ctrl (Windows/Linux) and click on a keyword within a `defmethod integrant.core/init-key` form.
3. You'll be navigated to the corresponding configuration in your EDN files.
4. The plugin intelligently prioritizes fully qualified keywords and direct configuration entries over references.

## Building from Source locally

```bash
./gradlew build
```
