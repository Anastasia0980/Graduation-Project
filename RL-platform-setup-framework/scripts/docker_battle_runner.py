#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Docker entrypoint for the existing battle_evaluator.py.

It keeps the current evaluator as the source of truth, maps container paths to
its existing CLI, and normalizes the final JSON to /output/result.json.
"""

import argparse
import json
from pathlib import Path
import subprocess
import sys
import traceback


DEFAULT_INPUT_DIR = "/input"
DEFAULT_OUTPUT_DIR = "/output"
DEFAULT_RESULT_BASE = "video_0"


def parse_args():
    parser = argparse.ArgumentParser(description="Run battle_evaluator.py inside Docker.")
    parser.add_argument("--input", default=DEFAULT_INPUT_DIR, help="Mounted input directory.")
    parser.add_argument("--output", default=DEFAULT_OUTPUT_DIR, help="Mounted output directory.")
    parser.add_argument("--student1-dir", default="student1", help="Student 1 submission directory under /input.")
    parser.add_argument("--student2-dir", default="student2", help="Student 2 submission directory under /input.")
    parser.add_argument("--env", default="tictactoe_v3")
    parser.add_argument("--games", type=int, default=30)
    parser.add_argument("--seed", type=int, default=None)
    parser.add_argument("--fps", type=int, default=5)
    return parser.parse_args()


def find_json_line(output_text):
    for line in reversed((output_text or "").splitlines()):
        line = line.strip()
        if line.startswith("{") and line.endswith("}"):
            return line
    return None


def write_json(path, payload):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, indent=2)


def validate_submission(input_dir, rel_dir, label):
    submission_dir = input_dir / rel_dir
    if not submission_dir.is_dir():
        raise FileNotFoundError(f"{label} directory not found: {submission_dir}")
    if not (submission_dir / "config.json").is_file():
        raise FileNotFoundError(f"{label} config.json not found in {submission_dir}")
    if not any((submission_dir / name).is_file() for name in ("model.pt", "model.pth")):
        raise FileNotFoundError(f"{label} model.pt/model.pth not found in {submission_dir}")
    return submission_dir


def append_file(log_file, title, source_file):
    if not source_file.is_file():
        return
    with log_file.open("a", encoding="utf-8") as log:
        log.write("\n")
        log.write(f"===== {title} =====\n")
        log.write(source_file.read_text(encoding="utf-8", errors="replace"))
        log.write("\n")


def build_failure(error_message, output_dir=Path(DEFAULT_OUTPUT_DIR)):
    return {
        "status": "FAILED",
        "winner": 0,
        "error": error_message,
        "result_dir": str(output_dir / DEFAULT_RESULT_BASE),
        "video": None,
    }


def main():
    args = parse_args()
    input_dir = Path(args.input).resolve()
    output_dir = Path(args.output).resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

    result_json = output_dir / "result.json"
    log_file = output_dir / "battle_log.txt"
    generated_result_json = output_dir / f"{DEFAULT_RESULT_BASE}_result.json"
    generated_summary = output_dir / f"{DEFAULT_RESULT_BASE}_summary.txt"

    try:
        validate_submission(input_dir, args.student1_dir, "student1")
        validate_submission(input_dir, args.student2_dir, "student2")

        script_path = Path(__file__).with_name("battle_evaluator.py")
        cmd = [
            sys.executable,
            str(script_path),
            "--workspace",
            str(input_dir),
            "--student1_dir",
            args.student1_dir,
            "--student2_dir",
            args.student2_dir,
            "--env",
            args.env,
            "--games",
            str(args.games),
            "--result_base",
            str(output_dir / DEFAULT_RESULT_BASE),
            "--fps",
            str(args.fps),
        ]
        if args.seed is not None:
            cmd.extend(["--seed", str(args.seed)])

        with log_file.open("w", encoding="utf-8") as log:
            log.write("[docker_battle_runner] command:\n")
            log.write(" ".join(cmd))
            log.write("\n\n")

        completed = subprocess.run(
            cmd,
            cwd=str(script_path.parent),
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            encoding="utf-8",
            errors="replace",
        )

        stdout_text = completed.stdout or ""
        with log_file.open("a", encoding="utf-8") as log:
            log.write(stdout_text)
            if stdout_text and not stdout_text.endswith("\n"):
                log.write("\n")
            log.write(f"[docker_battle_runner] exit_code={completed.returncode}\n")

        payload = None
        if generated_result_json.is_file():
            with generated_result_json.open("r", encoding="utf-8") as f:
                payload = json.load(f)
        else:
            json_line = find_json_line(stdout_text)
            if json_line:
                payload = json.loads(json_line)

        if payload is None:
            payload = build_failure("battle_evaluator.py did not produce a JSON result", output_dir)

        if completed.returncode != 0 and not payload.get("error"):
            payload["status"] = "FAILED"
            payload["error"] = f"battle_evaluator.py exited with code {completed.returncode}"

        write_json(result_json, payload)
        append_file(log_file, "video_0_summary.txt", generated_summary)
        print(json.dumps(payload, ensure_ascii=False))
        return completed.returncode

    except Exception as exc:
        error_message = f"{exc}\n{traceback.format_exc()}"
        payload = build_failure(str(exc), output_dir)
        write_json(result_json, payload)
        with log_file.open("a", encoding="utf-8") as log:
            log.write(error_message)
            log.write("\n")
        print(json.dumps(payload, ensure_ascii=False))
        return 2


if __name__ == "__main__":
    sys.exit(main())
