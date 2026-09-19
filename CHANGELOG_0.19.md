# 0.19

- Notices use a content-sized rounded TextView with centered text, a maximum width for long messages, and a fade animation. Native Toast is a fallback instead of a simultaneous duplicate.
- Expansion chevrons swap between up/down drawables without rotating their buttons. Applied to both the floating panel and startup guide.
- Export opens an XLSX-specific ACTION_VIEW chooser with URI read permission and ClipData. Missing viewers and launch errors are reported instead of ignored. Downloads storage is unchanged.
- Counting logic is unchanged from 0.18, confirmed by the user on their actual list.
