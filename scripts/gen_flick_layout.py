#!/usr/bin/env python3
"""Generates the default keyboard layouts from mozc's own romanji tables.

Layout JSON stores mozc *table keys* (ASCII), not the characters shown on the keys. Hand-writing
those is how you end up with a keyboard where one flick direction silently does nothing, so they
are derived here straight from third_party/mozc/src/data/preedit/*.tsv and validated against them.

Key arrangement follows Gboard's Japanese 12-key: a function column down the left (undo, cursor
left, layout switches) and a second one down the right (backspace, cursor right, space, enter),
with the ten character keys in the middle three columns.
"""

from __future__ import annotations

import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
PREEDIT = ROOT / "third_party/mozc/src/data/preedit"
KANA_TABLE = PREEDIT / "flick-hiragana.tsv"
ASCII_TABLE = PREEDIT / "toggle_flick-halfwidthascii.tsv"
SYMBOL_TABLE = PREEDIT / "toggle_flick-number.tsv"
OUT_DIR = ROOT / "app/src/main/assets/layouts"
GEOMETRY_OUT = ROOT / "patches/flick_geometry.inc"

KANA_ID = "flick_kana"
ASCII_ID = "flick_ascii"
SYMBOL_ID = "flick_symbol"

# Plane-switch keys are labelled with where they go, not with a mode name. Gboard's あa1 implies a
# three-way cycle and its ☺記 implies emoji; ours does neither, and a label that promises a plane
# the key does not open is worse than a plain one.
KANA_LABEL = "かな"
ASCII_LABEL = "英数"
SYMBOL_LABEL = "数字"


def load_table(path: pathlib.Path) -> dict[str, str]:
    """ASCII table key -> the character it produces, for the single-key base entries.

    The ascii table wraps output in `{*}` / `{?}` toggle markers; strip them, since only the
    resulting character matters for a key face.
    """
    mapping: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        parts = line.split("\t")
        if len(parts) < 3:
            continue
        key, pending, output = parts[0], parts[1], parts[2]
        output = output.replace("{*}", "").replace("{?}", "")
        if len(key) == 1 and pending == "" and output:
            mapping[key] = output
    return mapping


# ---------------------------------------------------------------- character keys

# (center, left, up, right, down) as mozc flick-hiragana table keys.
# い/う/え/お sit left/up/right/down of あ, which is the standard Japanese flick arrangement.
KANA_KEYS = {
    "a":    ("1", "_", ";", ":", "@"),
    "ka":   ("2", "a", "b", "c", "|"),
    "sa":   ("3", "d", "e", "f", "~"),
    "ta":   ("4", "g", "h", "i", "$"),
    "na":   ("5", "j", "k", "l", "%"),
    "ha":   ("6", "m", "n", "o", "&"),
    "ma":   ("7", "p", "q", "r", "s"),
    "ya":   ("8", "t", "u", "v", "^"),
    "ra":   ("9", "w", "x", "y", "z"),
    "wa":   ("0", "+", "/", "-", "<"),
    "punc": ("#", ",", "?", "!", ">"),
}

# Same shape for toggle_flick-halfwidthascii. Down is the digit, matching the small numeral Gboard
# prints under each letter group.
ASCII_KEYS = {
    "at":   ("1", "-", "_", "/", "@"),
    "abc":  ("2", "b", "c", None, "|"),
    "def":  ("3", "e", "f", None, "~"),
    "ghi":  ("4", "h", "i", None, "$"),
    "jkl":  ("5", "k", "l", None, "%"),
    "mno":  ("6", "n", "o", None, "&"),
    "pqrs": ("7", "q", "r", "s", "<"),
    "tuv":  ("8", "u", "v", None, "^"),
    "wxyz": ("9", "x", "y", "z", ">"),
    "quot": ("0", '"', ":", ";", "#"),
    "punc": (".", ",", "?", "!", None),
}

# Symbol plane, as mozc's toggle_flick-number table keys. Every symbol is its own key there, so
# these go through the table like any other character.
#
# Gboard reaches this plane from the ☺記 key and nothing else — it does not scatter symbols across
# the latin plane's spare flick directions. Following that keeps the latin keys predictable and
# gives the symbols somewhere they can be laid out properly.
SYMBOL_KEYS = {
    "1":     ("1", "☆", "♪", "→", None),
    "2":     ("2", "¥", "$", "€", None),
    "3":     ("3", "%", "゜", "#", None),
    "4":     ("4", "○", "*", "・", None),
    "5":     ("5", "+", "×", "÷", None),
    "6":     ("6", "<", "=", ">", None),
    "7":     ("7", "「", "」", ":", None),
    "8":     ("8", "〒", "々", "〆", None),
    "9":     ("9", "^", "|", "\\", None),
    "paren": ("(", ")", "[", "]", None),
    "0":     ("0", "〜", "…", "@", None),
    "punc":  (".", ",", "-", "/", None),
}

# '*' cycles, '`' forces small, '[' forces dakuten, ']' forces handakuten.
KANA_MODIFIER = ("*", "[", "`", "]")
KANA_MODIFIER_LABELS = ("゛小゜", "゛", "小", "゜")


def input_key(table: dict[str, str] | None, key: str | None) -> dict | None:
    """Builds a key output. `table` is None for layouts whose keys are literal."""
    if key is None:
        return None
    if table is None:
        label = key
    else:
        label = table.get(key)
        if not label:
            raise SystemExit(f"table key {key!r} has no output")
    return {"label": label, "action": {"type": "input", "text": key}}


def char_key(table: dict[str, str] | None, spec: tuple, label: str | None = None) -> dict:
    center, left, up, right, down = spec
    key = {"weight": 1.0, "center": input_key(table, center), "style": "CHARACTER"}
    for direction, table_key in (("left", left), ("up", up), ("right", right), ("down", down)):
        key[direction] = input_key(table, table_key)
    if label:
        key["label"] = label
    return key


def action_key(label: str, action: dict, style: str = "ACTION",
               repeatable: bool = False) -> dict:
    key = {"weight": 1.0, "center": {"label": label, "action": action}, "style": style}
    if repeatable:
        key["repeatable"] = True
    return key


# ---------------------------------------------------------------- function columns

UNDO = lambda: action_key("↶", {"type": "undo"}, style="MODIFIER")
BACKSPACE = lambda: action_key("⌫", {"type": "backspace"}, repeatable=True)
CURSOR_LEFT = lambda: action_key("◀", {"type": "cursor", "delta": -1}, style="MODIFIER",
                                 repeatable=True)
CURSOR_RIGHT = lambda: action_key("▶", {"type": "cursor", "delta": 1}, repeatable=True)


def SPACE():
    # Flick the space key sideways to nudge the caret: fine cursor control without leaving the
    # home position. A tap still spaces — only the swipe changes.
    key = action_key("␣", {"type": "space"}, repeatable=True)
    key["left"] = {"label": "◀", "action": {"type": "cursor", "delta": -1}}
    key["right"] = {"label": "▶", "action": {"type": "cursor", "delta": 1}}
    return key


ENTER = lambda: action_key("↵", {"type": "enter"})


def switch(label: str, layout_id: str) -> dict:
    return action_key(label, {"type": "layout", "layoutId": layout_id}, style="MODIFIER")


def build(layout_id: str, label: str, input_style: str, keys: list[dict],
          symbol_switch: dict, cycle_switch: dict) -> dict:
    """Assembles the Gboard-shaped grid from ten character keys plus one modifier key.

    `keys` is [k1..k9, modifier, k10, k11] — nine keys for the first three rows, then the bottom
    row's modifier and two remaining character keys.
    """
    k = keys
    rows = [
        {"keys": [UNDO(), k[0], k[1], k[2], BACKSPACE()]},
        {"keys": [CURSOR_LEFT(), k[3], k[4], k[5], CURSOR_RIGHT()]},
        {"keys": [symbol_switch, k[6], k[7], k[8], SPACE()]},
        {"keys": [cycle_switch, k[9], k[10], k[11], ENTER()]},
    ]
    return {"id": layout_id, "label": label, "inputStyle": input_style, "rows": rows}


def build_kana(table: dict[str, str]) -> dict:
    center, small, dakuten, handakuten = KANA_MODIFIER
    modifier = {
        "weight": 1.0,
        "label": KANA_MODIFIER_LABELS[0],
        "center": {"label": KANA_MODIFIER_LABELS[0], "action": {"type": "modify"}},
        "left": {"label": KANA_MODIFIER_LABELS[1], "action": {"type": "input", "text": small}},
        "up": {"label": KANA_MODIFIER_LABELS[2], "action": {"type": "input", "text": dakuten}},
        "right": {"label": KANA_MODIFIER_LABELS[3], "action": {"type": "input", "text": handakuten}},
        "style": "MODIFIER",
    }
    keys = [char_key(table, KANA_KEYS[n]) for n in
            ("a", "ka", "sa", "ta", "na", "ha", "ma", "ya", "ra")]
    keys += [modifier, char_key(table, KANA_KEYS["wa"]),
             char_key(table, KANA_KEYS["punc"], label="、。?!")]
    return build(KANA_ID, "かな (フリック)", "FLICK_HIRAGANA", keys,
                 switch(SYMBOL_LABEL, SYMBOL_ID), switch(ASCII_LABEL, ASCII_ID))


def build_ascii(table: dict[str, str]) -> dict:
    # 'a⇔A' is the same '*' cycle key as the kana plane's dakuten key — in the ascii table it
    # walks the preceding letter through its case variants instead.
    modifier = action_key("a⇔A", {"type": "modify"}, style="MODIFIER")

    def group(name: str, label: str) -> dict:
        return char_key(table, ASCII_KEYS[name], label=label)

    keys = [
        group("at", "@-_/"), group("abc", "ABC"), group("def", "DEF"),
        group("ghi", "GHI"), group("jkl", "JKL"), group("mno", "MNO"),
        group("pqrs", "PQRS"), group("tuv", "TUV"), group("wxyz", "WXYZ"),
        modifier, group("quot", "'\":;"), group("punc", ".,?!"),
    ]
    return build(ASCII_ID, "英数 (フリック)", "TOGGLE_FLICK_HALFWIDTH_ASCII", keys,
                 switch(SYMBOL_LABEL, SYMBOL_ID), switch(KANA_LABEL, KANA_ID))


def build_symbol(table: dict[str, str]) -> dict:
    keys = [char_key(table, SYMBOL_KEYS[n])
            for n in ("1", "2", "3", "4", "5", "6", "7", "8", "9")]
    keys += [char_key(table, SYMBOL_KEYS["paren"], label="()[]"),
             char_key(table, SYMBOL_KEYS["0"]),
             char_key(table, SYMBOL_KEYS["punc"], label=".,-/")]
    # Gboard puts a second symbol page behind !?# here. We have no such page yet, so the slot goes
    # back to kana rather than advertising something that does not exist.
    return build(SYMBOL_ID, "記号・数字", "TOGGLE_FLICK_NUMBER", keys,
                 switch(KANA_LABEL, KANA_ID), switch(ASCII_LABEL, ASCII_ID))


# A kana is identified by which key it sits on and which flick direction reaches it. Keeping those
# two apart matters: landing on the wrong key and flicking the wrong way are different mistakes, and
# collapsing them into one coordinate makes the down-flick of one row land exactly on the up-flick
# of the row below — which would make の and ゆ the same point.
DIRECTION_INDEX = {"center": 0, "left": 1, "up": 2, "right": 3, "down": 4}


def write_geometry(layout: dict) -> None:
    """Emits the kana -> (key column, key row, flick direction) table the corrector ranks by.

    Derived from the layout that ships, so the corrector's idea of which keys are neighbours cannot
    drift away from where the keys actually are.
    """
    entries: dict[str, tuple[int, int, int]] = {}
    for row_index, row in enumerate(layout["rows"]):
        for col_index, key in enumerate(row["keys"]):
            for direction, dir_index in DIRECTION_INDEX.items():
                output = key.get(direction)
                if output is None:
                    continue
                if output["action"].get("type") != "input":
                    continue
                label = output["label"]
                # Hiragana and the長音 mark only. Punctuation shares keys with kana — "(" and ")"
                # sit on the や key — and letting it into the neighbour set would spend the
                # corrector's small candidate budget proposing readings with brackets in them.
                if not all("\u3041" <= ch <= "\u3096" or ch == "ー" for ch in label):
                    continue
                # Keep the first placement of a kana; duplicates would only add noise.
                entries.setdefault(label, (col_index, row_index, dir_index))

    lines = [
        "// Generated by scripts/gen_flick_layout.py — do not edit.",
        "// kana, key column, key row, flick direction (0=centre,1=left,2=up,3=right,4=down).",
        "",
    ]
    for kana, (col, row, direction) in sorted(entries.items()):
        lines.append(f'{{"{kana}", {col}, {row}, {direction}}},')
    GEOMETRY_OUT.parent.mkdir(parents=True, exist_ok=True)
    GEOMETRY_OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {GEOMETRY_OUT} ({len(entries)} kana)")


def main() -> int:
    for table_path in (KANA_TABLE, ASCII_TABLE, SYMBOL_TABLE):
        if not table_path.is_file():
            print(f"error: {table_path} not found; clone mozc first", file=sys.stderr)
            return 1

    layouts = [
        build_kana(load_table(KANA_TABLE)),
        build_ascii(load_table(ASCII_TABLE)),
        build_symbol(load_table(SYMBOL_TABLE)),
    ]

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for layout in layouts:
        out = OUT_DIR / f"{layout['id']}.json"
        out.write_text(json.dumps(layout, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"wrote {out}")

    write_geometry(layouts[0])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
