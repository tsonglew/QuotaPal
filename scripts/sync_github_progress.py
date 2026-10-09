#!/usr/bin/env python3
"""Sync existing GitHub tracking issues and milestones from local progress documents."""
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
            "实现与验证见 https://github.com/tsonglew/QuotaPal/pull/8 。\n\n")
    body = re.sub(r"## 当前进度\n.*?(?=完整计划)", note, body, flags=re.S)
    with tempfile.NamedTemporaryFile(mode="w", suffix=".md") as output:
        output.write(body)
        output.flush()
        subprocess.run(["gh", "issue", "edit", str(number), "--repo", "tsonglew/QuotaPal",
                        "--body-file", output.name], check=True, stdout=subprocess.DEVNULL)
    endpoint = f"repos/tsonglew/QuotaPal/milestones/{number}"
    milestone = json.loads(subprocess.check_output(["gh", "api", endpoint]))
    description = re.sub(r"状态：[^。]+。", f"状态：{status.strip()}。", milestone["description"], count=1)
    description = description.replace("尚未确定开工日期，不设置截止日期。",
        "已于 2026-10-09 启动；暂不设置截止日期。" if number < 6 else "开工日期待前置验收后确定；暂不设置截止日期。")
    description = description.replace("范围：后续实施", "范围：实施")
    description = description.replace("本里程碑仅记录未来工作，创建里程碑不代表已初始化工程或开始编码。",
        "实现与自动验证已完成；手机真实账号闭环仍须结合 M0 验收。")
    description = description.replace("/blob/master/docs/", "/blob/codex/android-mvp/docs/")
    description = re.sub(r"\n\n## 当前进展\n.*", "", description, flags=re.S)
    description += (f"\n\n## 当前进展\n\n2026-10-09：{evidence.strip()}。下一步：{next_step.strip()}。\n\n"
        f"[跟踪任务 #{number}](https://github.com/tsonglew/QuotaPal/issues/{number}) · "
        "[实时进度](https://github.com/tsonglew/QuotaPal/blob/codex/android-mvp/docs/PROGRESS.md) · "
        "[验证记录](https://github.com/tsonglew/QuotaPal/blob/codex/android-mvp/docs/VALIDATION_ALPHA03.md)")
    with tempfile.NamedTemporaryFile(mode="w", suffix=".json") as output:
        json.dump({"description": description}, output, ensure_ascii=False)
        output.flush()
        subprocess.run(["gh", "api", endpoint, "--method", "PATCH", "--input", output.name],
                       check=True, stdout=subprocess.DEVNULL)
