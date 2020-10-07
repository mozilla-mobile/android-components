#!/usr/bin/python3

import sys
import argparse
from pathlib import Path
import json
import yaml

def parse_args(cmdln_args):
    parser = argparse.ArgumentParser(description="Parse UI test logs an results")
    parser.add_argument(
        "--output-md",
        type=argparse.FileType("w", encoding="utf-8"),
        help="Output markdown file.",
        required=True,
    )
    parser.add_argument(
        "--log",
        type=argparse.FileType("r", encoding="utf-8"),
        help="Log output of flank.",
        required=True,
    )
    parser.add_argument("--device-type", help="Type of device ", required=True)
    return parser.parse_args(args=cmdln_args)


def extract_android_args(log):
    return yaml.safe_load(log.split("AndroidArgs\n")[1].split("RunTests\n")[0])


def format_test_results_to_markdown(devices, matrix_results_per_id):
    markdown_lines = [
        # insert each print statement without \n, here
    ]

    markdown_lines.extend([
        "| {matrixId} | {outcome} | [logs]({webLink}) |".format(**matrix_result)
        for matrix_results in matrix_results_per_id.values()
    ])

    return "\n".join(markdown_lines)


def main():
    args = parse_args()
    log = args.log.read()
    android_args = extract_android_args(log)

    matrix_ids = json.loads(args.results.joinpath("matrix_ids.json").read_text())

    print("# Devices\n")
    print(yaml.safe_dump(android_args["gcloud"]["device"]))

    print("# Results\n")
    print("| matrix | result | logs |\n")
    print("| --- | --- | --- |\n")
    #for matrix, matrix_result in matrix_ids.items():
    for matrix, matrix_results_per_id in matrix_ids.items():
        #print("| {matrixId} | {outcome} | [logs]({webLink}) |\n".format(**matrix_result))
        markdown = format_test_results_to_markdown(devices, matrix_results_per_id):
        args.output_md.write(markdown)


if __name__ == "__main__":
    main()


