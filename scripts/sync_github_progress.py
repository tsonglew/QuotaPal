#!/usr/bin/env python3
"""Sync existing GitHub tracking issues from the reviewed local progress documents."""
import argparse
import json
import re
import subprocess
import tempfile
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("--apply", action="store_true", help="Apply updates; otherwise only print statuses")
args = parser.parse_args()
root = Path(__file__).resolve().parent.parent
checklist = (root / "docs/DEVELOPMENT_CHECKLIST.md").read_text()
progress = (root / "docs/PROGRESS.md").read_text()
states = {item: mark for mark, item in re.findall(r"- \[([ x])\] \*\*([A-F]\d\d)\*\*", checklist)}
rows = re.findall(r"\| M([0-5]) [^|]+\| ([^|]+)\| ([^|]+)\| ([^|]+)\|", progress)
for stage, status, evidence, next_step in rows:
    number = int(stage) + 1
    print(f"#{number} M{stage}: {status.strip()}")
    if not args.apply:
        continue
    issue = json.loads(subprocess.check_output([
        "gh", "issue", "view", str(number), "--repo", "tsonglew/QuotaPal", "--json", "body"
    ]))
    body = re.sub(r"当前状态：[^。]+。", f"当前状态：{status.strip()}。", issue["body"], count=1)
    body = re.sub(r"- \[[ x]\] \*\*([A-F]\d\d)\*\*", lambda match:
                  f"- [{states.get(match[1], ' ')}] **{match[1]}**", body)
    note = (f"## 当前进度\n\n2026-10-09：{evidence.strip()}。下一步：{next_step.strip()}。\n\n"
            "实现与验证见 https://github.com/tsonglew/QuotaPal/pull/7 。\n\n")
    body = re.sub(r"## 当前进度\n.*?(?=完整计划)", note, body, flags=re.S)
    with tempfile.NamedTemporaryFile(mode="w", suffix=".md") as output:
        output.write(body)
        output.flush()
        subprocess.run(["gh", "issue", "edit", str(number), "--repo", "tsonglew/QuotaPal",
                        "--body-file", output.name], check=True, stdout=subprocess.DEVNULL)
