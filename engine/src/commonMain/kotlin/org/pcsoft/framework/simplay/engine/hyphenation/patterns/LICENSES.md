# Hyphenation Pattern Licenses

The raw TeX hyphenation pattern files (`hyph-<locale>.pat.txt`) in this directory are taken
unmodified from the [hyph-utf8 project](https://github.com/hyphenation/tex-hyphen)
(`github.com/hyphenation/tex-hyphen`), originally distributed via CTAN
(The Comprehensive TeX Archive Network).

Each pattern file's license was verified against the YAML-style header of the corresponding
`.tex` source file in the hyph-utf8 repository
(`hyph-utf8/tex/generic/hyph-utf8/patterns/tex/<name>.tex`) - the plain `.pat.txt` data files
carry no license header of their own. Only files with a permissive license (MIT, BSD, Public
Domain, or LPPL 1.2+) are bundled.

## Bundled locales

| Locale | Source file | License | Status |
|--------|-------------|---------|--------|
| de | hyph-de-1996.tex | MIT | Included |
| en | hyph-en-us.tex | Custom permissive (free copying/distribution with notice) | Included |

## Critical / rejected licenses seen during evaluation

The scope of this project currently covers only `de` and `en`. While surveying the wider
hyph-utf8 pattern set for possible future languages, several files turned out to carry
licenses that are **incompatible** with this Apache-2.0 project and must **not** be bundled
without a separate decision:

| Locale | Source file | License | Problem |
|--------|-------------|---------|---------|
| cs | hyph-cs.tex | GPL 2 or later | Copyleft |
| hu | hyph-hu.tex | MPL 1.1 / GPL 2.0 / LGPL 2.1 (choice) | No permissive option in the license choice |
| hy | hyph-hy.tex | LGPL 3.0 | Copyleft |
| id | hyph-id.tex | GPL 2 | Copyleft |
| lv | hyph-lv.tex | LGPL 2.1 or GPL 2+ (choice) | Copyleft |
| mk | hyph-mk.tex | GPL | Copyleft |
| sr | hyph-sr-cyrl.tex | GPL | Copyleft |
| fi | hyph-fi.tex | "Patterns may be freely distributed" | No clear, named license |
| ro | hyph-ro.tex | License field explicitly "[None]" | No license granted at all |

Should additional languages be added later, each candidate's `.tex` header license must be
re-verified individually against this list before its `.pat.txt` file is copied here.
