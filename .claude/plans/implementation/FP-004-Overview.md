# Overview: FP-004 Konsolen-Bitmap-Fonts

* Feature Plan: `.claude/plans/features/FP-004-ConsoleBitmapFonts.md`

## Implementierungspläne

* IP-01: FLF-Format-Parser & Modell - `FP-004-IP-01-FlfFormatParser.md`
* IP-02: Mitgelieferter Font-Satz - `FP-004-IP-02-BundledFontSet.md`
* IP-03: Externes Nachladen eigener Fonts - `FP-004-IP-03-ExternalFontLoading.md`
* IP-04: Glyphen-Rendering-Integration - `FP-004-IP-04-GlyphRenderingIntegration.md`
* IP-05: Font-Registry-Integration & öffentliche API - `FP-004-IP-05-FontRegistryIntegration.md`

## Umsetzungsreihenfolge

* IP-01 zuerst (unabhängig)
* IP-02, IP-03 und IP-04 danach parallel möglich
  * IP-04 zusätzlich abhängig von FP-003 IP-02
* IP-05 zuletzt (abhängig von IP-02, IP-03, IP-04)
