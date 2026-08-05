# Insane Blade · 八刀 — Store Page (English)

> Paste-ready for CurseForge / Modrinth. Suggested summary line first, then the long description.

## Summary (short, one-liner)

Your whole hotbar joins the fight: the other 8 SlashBlades roll to strike with you — with icon bursts, kill-feed flair, and a full-proc JACKPOT.

## Description (long)

**Insane Blade · 八刀** is a combat addon for SlashBlade-flavored mods (works with *SlashBlade: Resharped* and any mod whose blades live in the `slashblade` namespace) that turns your hotbar into a blade formation. When you land a melee hit with a blade in your main hand, each of the **other 8 blades in your hotbar** independently rolls a chance to join the strike, adding its own damage on top.

### Proc engine — PRD + Momentum

No pure dice here:

- **PRD (pseudo-random distribution, Dota 2 style):** every blade tracks its own miss streak — the longer it misses, the higher its next proc chance, ramping to a guaranteed hit. Long-term proc rate is tunable via the PRD constant (~15–25% out of the box).
- **Momentum:** when any blade procs, a short window opens that boosts *all* blades' proc chance, stacking up to a cap and refreshing on each proc. That's where those "everything fired at once" highlight moments come from — by design, not luck.

### Damage models — pick your flavor (server config)

- **FLAT** (default): each procced blade adds `its damage × damageScale`. Predictable, zero surprises.
- **RATIO**: bonus scales directly with your main hand's final damage — all your modpack buffs apply proportionally.
- **BLEND**: geometric blend of blade damage and main-hand damage (`y^(1-α) · x0^α`). Buff-scaling with a built-in anti-runaway curve; identical blades behave exactly like FLAT.
- **Safety fuse:** `maxBonusRatio` caps the total bonus relative to your main-hand hit, so stacked-buff modpacks can't spiral out of control.

Cooldown scaling is respected, and the bonus is applied before armor reduction.

### Feedback — three overlay styles (client config)

- **BURST** — icons pop and float up above your hotbar.
- **KILLFEED** — a scrolling feed slides in on the right, one row per proc group.
- **MINIMAL** — just a quick counter flash.

Proc counts tier up: 1–4 gold, 5–7 bigger red, and a full 8-blade **JACKPOT** shows a dedicated emblem with 「疯狂！！」 instead of a number. Slash sound effects pitch up with the proc count. Icon scale, linger time, volume, and per-player toggles are all in the client config — on servers, every player gets their own preferred flavor.

### Multiplayer & performance

- All damage logic is server-authoritative (no cheating surface); vanilla clients can join a server running it — they just won't see the feedback.
- Server rules live in `serverconfig`, visuals in per-player `config/insaneblade-client.toml`.
- Cost per hit: 8 item reads + 8 RNG rolls + at most one packet. Nanoseconds. No entities spawned, ever.

### Requirements

- Minecraft 1.20.1, Forge (also runs on NeoForge 1.20.1 47.1.x)
- A SlashBlade mod to supply the blades (e.g. *SlashBlade: Resharped*) — without one, the mod simply stays dormant.

Open source under MIT. Issues and PRs welcome on GitHub.
