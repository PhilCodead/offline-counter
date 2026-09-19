# 0.18

- Restored the 0.12 counting sequence from `generated_smali/CounterService.smali`: one timer; 140 ms rewind, 180 ms phase transition, 300 ms forward scan; scan before scrolling; full-screen fingerprint; unique name/date keys; two unchanged screens or five scans with no new people terminate a stationary list.
- Accessibility events now only select the source application. They cannot reschedule the counting timer or supply stale top/bottom indices.
- Each cycle captures text once for both parsing and the fingerprint. Every requested scroll receives exactly one result before another frame is accepted.
- The 400-rewind/600-forward safety limits report an incomplete result instead of presenting a partial count as successful. These are deliberate differences from 0.12.
- Added repeated-run coverage: 85 complete passes over 37 people from 17 starting positions, overlapping pages, rejected scrolls, stationary lists, stop/clear and unavailable source.
- Free placement is persisted. Snapping only occurs within 24 dp of either edge; resizing clamps free panels and retains attached edges.
- Replaced the Material card and layout transitions with one rounded charcoal background and transparent children. Action buttons fade in without transition ghost layers.

Verification requires CI unit tests, lint and APK assembly. Accessibility timing and visual appearance on the user's Samsung device still require a real-device check. No claim is made that synthetic lists prove the real source application's total.
