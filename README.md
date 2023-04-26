# blabla

Chat with an LLM from the command line.

A tiny babashka (Clojure) wrapper around the OpenAI API.

## Installation

1. Install babashka
2. Clone this repo (or copy `blabla.clj`)
3. Create a config file under `$HOME/.config/blabla/config.edn` :
    ```edn
    {:open-ai-key "sk-..."
     :model "gpt-4o}
    ```
4. Create a symlink in a directory in your PATH, e.g.:
    ```bash
    ln -s /path/to/blabla.clj ~/.local/bin/blabla
    ```

## Usage

```bash
blabla

> How can I simulate a d20 if I only have a d6?

Use base‑6 to get 20 distinct outcomes with minimal bias.

One simple method (3d6):

1. Roll three d6: call them A, B, C (each 1–6).
2. Compute `N = (A−1)*36 + (B−1)*6 + (C−1)` → range 0–215.
3. If `N ≥ 200`, discard and reroll all three dice.
4. Otherwise, output `N mod 20 + 1` as the d20 result.

That’s exact and uniform, just with occasional rejection.

>
```

`Ctrl+C` to stop.

