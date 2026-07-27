#!/usr/bin/env python3
"""Keeps only the transliterations in the katakana-English dictionary.

The upstream data is derived from EDICT, which is a *translation* dictionary. That mixes two very
different things under one reading:

    ぶらっく  -> black                                          wanted: the kana spell the English
    くろ      -> black                                          unwanted: the Japanese word for it
    そ        -> 5th note in the tonic solfa representation ...  unwanted: an entire definition

Only the first kind belongs in an IME. The other two turn ordinary Japanese input into a stream of
English glosses — typing それ was offering the solfa definition, because EDICT files そ that way.

Telling them apart phonetically works because Japanese cannot end a syllable on most consonants:
writing an English word in kana forces a vowel in after each one. Throw those vowels away and the
consonants line up.

    ぶらっく -> burakku -> b r k k -> brk
    black             -> b l k     -> brk   (l and r are the same sound here)

A translation has no such relationship: くろ gives "kr" against black's "brk", あぶらむし gives
"brms" against "plant louse". Scoring the two skeletons against each other separates the classes
cleanly — genuine transliterations land at 0.67 and above, translations at 0.50 and below.

Reads the raw dictionary on stdin, writes the filtered one to stdout, reports to stderr.
"""

import difflib
import re
import sys

# Hiragana to romaji. Digraphs first so they win over their parts.
ROMAJI = {
    "きゃ": "kya", "きゅ": "kyu", "きょ": "kyo", "しゃ": "sha", "しゅ": "shu", "しょ": "sho",
    "ちゃ": "cha", "ちゅ": "chu", "ちょ": "cho", "にゃ": "nya", "にゅ": "nyu", "にょ": "nyo",
    "ひゃ": "hya", "ひゅ": "hyu", "ひょ": "hyo", "みゃ": "mya", "みゅ": "myu", "みょ": "myo",
    "りゃ": "rya", "りゅ": "ryu", "りょ": "ryo", "ぎゃ": "gya", "ぎゅ": "gyu", "ぎょ": "gyo",
    "じゃ": "ja", "じゅ": "ju", "じょ": "jo", "ぢゃ": "ja", "ぢゅ": "ju", "ぢょ": "jo",
    "びゃ": "bya", "びゅ": "byu", "びょ": "byo", "ぴゃ": "pya", "ぴゅ": "pyu", "ぴょ": "pyo",
    "うぃ": "wi", "うぇ": "we", "うぉ": "wo", "ゔぁ": "va", "ゔぃ": "vi", "ゔぇ": "ve",
    "ゔぉ": "vo", "ふぁ": "fa", "ふぃ": "fi", "ふぇ": "fe", "ふぉ": "fo", "ふゅ": "fyu",
    "てぃ": "ti", "でぃ": "di", "とぅ": "tu", "どぅ": "du", "しぇ": "she", "じぇ": "je",
    "ちぇ": "che", "つぁ": "tsa", "つぃ": "tsi", "つぇ": "tse", "つぉ": "tso",
    "あ": "a", "い": "i", "う": "u", "え": "e", "お": "o",
    "か": "ka", "き": "ki", "く": "ku", "け": "ke", "こ": "ko",
    "さ": "sa", "し": "shi", "す": "su", "せ": "se", "そ": "so",
    "た": "ta", "ち": "chi", "つ": "tsu", "て": "te", "と": "to",
    "な": "na", "に": "ni", "ぬ": "nu", "ね": "ne", "の": "no",
    "は": "ha", "ひ": "hi", "ふ": "fu", "へ": "he", "ほ": "ho",
    "ま": "ma", "み": "mi", "む": "mu", "め": "me", "も": "mo",
    "や": "ya", "ゆ": "yu", "よ": "yo",
    "ら": "ra", "り": "ri", "る": "ru", "れ": "re", "ろ": "ro",
    "わ": "wa", "ゐ": "i", "ゑ": "e", "を": "o", "ん": "n",
    "が": "ga", "ぎ": "gi", "ぐ": "gu", "げ": "ge", "ご": "go",
    "ざ": "za", "じ": "ji", "ず": "zu", "ぜ": "ze", "ぞ": "zo",
    "だ": "da", "ぢ": "ji", "づ": "zu", "で": "de", "ど": "do",
    "ば": "ba", "び": "bi", "ぶ": "bu", "べ": "be", "ぼ": "bo",
    "ぱ": "pa", "ぴ": "pi", "ぷ": "pu", "ぺ": "pe", "ぽ": "po",
    "ゔ": "vu",
    "ぁ": "a", "ぃ": "i", "ぅ": "u", "ぇ": "e", "ぉ": "o",
    "ゃ": "ya", "ゅ": "yu", "ょ": "yo", "ゎ": "wa",
    "ー": "",  # a long vowel adds no consonant
}

# EDICT hangs its annotations off the end: "cookie (browser-related file...)", "(ger: Antimon)",
# "(和製: game center)". The head of the field is the actual spelling and is worth keeping.
PAREN = re.compile(r"\([^)]*\)")

# What a plausible English spelling may contain once the annotations are gone.
SPELLING = re.compile(r"[A-Za-z0-9][A-Za-z0-9 '\-.&/]*")

# The gap observed in the source is fairly sharp: real loanword spellings normally score 0.67 or
# better, while translations/glosses are generally 0.50 or worse. 0.55 admitted thousands of
# semantically related English translations (e.g. あざらし -> "true seal"), which are especially
# disruptive because this file is imported as a high-priority user dictionary.
THRESHOLD = 0.67


def romanize(kana):
    """Hiragana to romaji, or None if anything is not kana."""
    out = []
    i = 0
    while i < len(kana):
        if kana[i] == "っ":
            # A geminate doubles the following consonant and carries no vowel of its own; the
            # skeleton collapses runs anyway, so dropping it here is enough.
            i += 1
            continue
        two = kana[i:i + 2]
        if two in ROMAJI:
            out.append(ROMAJI[two])
            i += 2
            continue
        one = kana[i]
        if one not in ROMAJI:
            return None
        out.append(ROMAJI[one])
        i += 1
    return "".join(out)


def skeleton_ja(romaji):
    s = romaji
    s = s.replace("sh", "s").replace("ch", "t").replace("ts", "t").replace("j", "z")
    # Palatalisation (kya, ryu) is an artefact of the kana, not a consonant English has. A y after
    # a vowel or at the start is a real one, as in York, so only the post-consonant case goes.
    s = re.sub(r"(?<=[bdfghkmnprstwz])y", "", s)
    s = re.sub(r"[aeiou]", "", s)
    s = s.replace("z", "s")  # loanwords voice freely: resolution -> rezoryuushon
    return re.sub(r"(.)\1+", r"\1", s)


def skeleton_en(word):
    s = re.sub(r"[^a-z]", "", word.lower())
    if not s:
        return ""
    s = s.replace("ph", "f").replace("th", "s").replace("sh", "s").replace("ch", "t")
    s = s.replace("ck", "k").replace("qu", "k").replace("x", "ks")
    s = re.sub(r"c(?=[eiy])", "s", s)
    s = s.replace("c", "k").replace("q", "k")
    # Distinctions Japanese does not make.
    s = s.replace("l", "r").replace("v", "b").replace("j", "z")
    s = re.sub(r"[aeiou]", "", s)
    s = s.replace("z", "s")
    return re.sub(r"(.)\1+", r"\1", s)


def transliteration_score(reading, word):
    """0..1, how much the English looks like a reading of the kana rather than a translation."""
    romaji = romanize(reading)
    if romaji is None:
        return 0.0
    ja, en = skeleton_ja(romaji), skeleton_en(word)
    if not ja or not en:
        return 0.0
    return difflib.SequenceMatcher(None, ja, en).ratio()


def spelling_of(word):
    """The bare spelling from an EDICT value, or None if the field is prose."""
    head = PAREN.sub(" ", word)
    # Everything after a comma or semicolon is a further sense, not part of the spelling.
    head = re.split(r"[,;]", head)[0]
    head = re.sub(r"\s+", " ", head).strip(" -")
    return head if head and SPELLING.fullmatch(head) else None


def main():
    kept = {}
    read = dropped = 0
    for line in sys.stdin:
        fields = line.rstrip("\n").split("\t")
        if len(fields) < 3:
            continue
        read += 1
        reading, word, pos = fields[0], fields[1], fields[2]
        spelling = spelling_of(word)
        if spelling is None or transliteration_score(reading, spelling) < THRESHOLD:
            dropped += 1
            continue
        # Stripping the annotations collapses several senses onto one spelling.
        kept.setdefault((reading, spelling), pos)

    print("# Adapted from KEINOS/google-ime-user-dictionary-ja-en")
    print("# Source: https://github.com/KEINOS/google-ime-user-dictionary-ja-en")
    print("# License: CC BY-SA 3.0 https://creativecommons.org/licenses/by-sa/3.0/")
    print("# Changes: merged upstream parts, removed non-TSV data, filtered translations,")
    print("# normalized English spellings, and sorted/deduplicated entries.")
    print("# This adapted dictionary is distributed under CC BY-SA 3.0.")
    for (reading, spelling), pos in sorted(kept.items()):
        print("%s\t%s\t%s" % (reading, spelling, pos))

    sys.stderr.write(
        "    %d entries read, %d dropped as translations, %d kept\n"
        % (read, dropped, len(kept))
    )


if __name__ == "__main__":
    main()
