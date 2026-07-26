#!/usr/bin/env python3
"""Generates the qwerty family of layouts into app/src/main/assets/layouts/.

Four planes that switch between each other, arranged the way Gboard arranges them so the muscle
memory transfers:

    qwerty_kana     romaji in, kana out — mozc's QWERTY_HIRAGANA table composes ka -> か
    qwerty_ascii    plain latin, with a shift key
    qwerty_symbol   the ?123 page: digits and common punctuation
    qwerty_symbol2  the =\\< page: currency, maths and bracket symbols

Every row is padded to the same total width, so the columns line up between rows. Without that a
nine-key home row is stretched over the same span as the ten-key row above it and the keys drift
out from under the thumb.

Symbol keys emit `symbol` (verbatim insert) rather than `input`: mozc's romanji tables reuse
punctuation as table keys — in the latin table `<` selects 7 — so anything sent as input would come
back as something else entirely.
"""

import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
DEST = ROOT / "app/src/main/assets/layouts"

WIDTH = 10.0  # every row totals this, padding included


def key(label, action, weight=1.0, style="CHARACTER", repeatable=False, face=None):
    spec = {"weight": weight, "center": {"label": label, "action": action}}
    if face is not None:
        spec["label"] = face
    if repeatable:
        spec["repeatable"] = True
    if style != "CHARACTER":
        spec["style"] = style
    return spec


def letter(ch):
    return key(ch, {"type": "input", "text": ch})


def symbol(ch, face=None):
    return key(ch, {"type": "symbol", "text": ch}, face=face)


def switch(label, layout_id, weight=1.0):
    return key(label, {"type": "layout", "layoutId": layout_id}, weight, "MODIFIER")


BACKSPACE = key("⌫", {"type": "backspace"}, 1.5, "MODIFIER", repeatable=True)
ENTER = key("↵", {"type": "enter"}, 2.0, "ACTION")
SHIFT = key("⇧", {"type": "shift"}, 1.5, "MODIFIER")


def row(keys, pad_start=0.0, pad_end=None):
    """A row, with the trailing pad derived so the total always comes to WIDTH."""
    used = pad_start + sum(k["weight"] for k in keys)
    if pad_end is None:
        pad_end = WIDTH - used
    out = {"keys": keys}
    if pad_start:
        out["padStart"] = round(pad_start, 3)
    if pad_end > 1e-6:
        out["padEnd"] = round(pad_end, 3)
    assert abs(used + pad_end - WIDTH) < 1e-6, (used, pad_end)
    return out


def space_row(leading, trailing, middle=(), space_label="␣", space_action=None):
    """Bottom row: plane switches at the left, enter at the right, space filling what is left."""
    keys = list(leading) + list(middle)
    fixed = sum(k["weight"] for k in keys) + sum(k["weight"] for k in trailing)
    space = key(space_label, space_action or {"type": "space"}, WIDTH - fixed,
                repeatable=True)
    return row(keys + [space] + list(trailing))


def layout(layout_id, label, input_style, rows):
    return {"id": layout_id, "label": label, "inputStyle": input_style, "rows": rows}


TOP = "qwertyuiop"
HOME = "asdfghjkl"
BOTTOM = "zxcvbnm"


def qwerty_kana():
    # No shift here. mozc's romaji table does not compose upper case — "Tokyo" stays "Tokyo"
    # rather than becoming ときょ — so a shift key on this plane would silently stop producing
    # kana. Capitals live on the ascii plane, which is one key away.
    return layout(
        "qwerty_kana", "かな (QWERTY)", "QWERTY_HIRAGANA",
        [
            row([letter(c) for c in TOP]),
            row([letter(c) for c in HOME], pad_start=0.5),
            row([key("↶", {"type": "undo"}, 1.5, "MODIFIER")]
                + [letter(c) for c in BOTTOM] + [BACKSPACE]),
            space_row(
                leading=[switch("英数", "qwerty_ascii", 1.4),
                         key("、", {"type": "input", "text": ","}, 1.0)],
                trailing=[key("。", {"type": "input", "text": "."}, 1.0), ENTER],
                space_label="空白",
                space_action={"type": "convert"},
            ),
        ],
    )


def qwerty_ascii():
    return layout(
        "qwerty_ascii", "英数 (QWERTY)", "QWERTY_HALFWIDTH_ASCII",
        [
            row([letter(c) for c in TOP]),
            row([letter(c) for c in HOME], pad_start=0.5),
            row([SHIFT] + [letter(c) for c in BOTTOM] + [BACKSPACE]),
            space_row(
                leading=[switch("?123", "qwerty_symbol", 1.4),
                         switch("かな", "qwerty_kana", 1.2),
                         symbol(",")],
                trailing=[symbol("."), ENTER],
            ),
        ],
    )


def qwerty_symbol():
    return layout(
        "qwerty_symbol", "記号 (QWERTY)", "QWERTY_HALFWIDTH_ASCII",
        [
            row([symbol(c) for c in "1234567890"]),
            row([symbol(c) for c in "@#$_&-+()/"]),
            row([switch("=\\<", "qwerty_symbol2", 1.5)]
                + [symbol(c) for c in "*\"':;!?"] + [BACKSPACE]),
            space_row(
                leading=[switch("ABC", "qwerty_ascii", 1.5), symbol(",")],
                trailing=[symbol("."), ENTER],
            ),
        ],
    )


def qwerty_symbol2():
    return layout(
        "qwerty_symbol2", "記号 2 (QWERTY)", "QWERTY_HALFWIDTH_ASCII",
        [
            row([symbol(c) for c in "~`|•√π÷×¶Δ"]),
            row([symbol(c) for c in "£¢€¥^°={}"], pad_start=0.5),
            row([switch("?123", "qwerty_symbol", 1.5)]
                + [symbol(c) for c in "\\©®™%[]"] + [BACKSPACE]),
            space_row(
                leading=[switch("ABC", "qwerty_ascii", 1.5), symbol(","), symbol("<")],
                trailing=[symbol(">"), symbol("."), ENTER],
            ),
        ],
    )


def main():
    DEST.mkdir(parents=True, exist_ok=True)
    for build in (qwerty_kana, qwerty_ascii, qwerty_symbol, qwerty_symbol2):
        data = build()
        path = DEST / (data["id"] + ".json")
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
        rows = len(data["rows"])
        keys = sum(len(r["keys"]) for r in data["rows"])
        print("  %-16s %d rows, %d keys -> %s" % (data["id"], rows, keys, path.name))


if __name__ == "__main__":
    main()
