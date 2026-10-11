"""Publish an already verified master APK as an immutable build prerelease."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess


def publish(directory, source, run_id, attempt, repo, execute=subprocess.run):
    def gh(*args):
        return execute(['gh', *args], check=True, capture_output=True, text=True).stdout.strip()

    if not re.fullmatch(r'[0-9a-f]{40}', source):
        raise ValueError('Invalid source')
    if not all(re.fullmatch(r'[1-9][0-9]*', value) for value in (run_id, attempt)):
        raise ValueError('Invalid build identity')
    if not re.fullmatch(r'[\w.-]+/[\w.-]+', repo):
        raise ValueError('Invalid repository')
    directory = Path(directory)
    provenance = json.loads((directory / 'provenance.json').read_text())
    version_tag = provenance['tag']
    if not re.fullmatch(r'v\d+\.\d+\.\d+(?:-[0-9A-Za-z][0-9A-Za-z.-]*)?', version_tag):
        raise ValueError('Invalid version')
    if provenance['source_sha'] != source or provenance['mode'] != 'signing-check':
        raise ValueError('Unexpected signed source')
    apk = directory / f'QuotaPal-{version_tag[1:]}.apk'
    if apk.is_symlink() or not apk.is_file():
        raise ValueError('Invalid APK')
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    if digest != provenance['sha256'] or (directory / 'SHA256SUMS').read_text() != f'{digest}  {apk.name}\n':
        raise ValueError('APK checksum mismatch')
    # Each workflow attempt gets its own tag; published assets are never replaced.
    tag = f'build-{run_id}-{attempt}'
    notes = directory / 'build-release-notes.md'
    notes.write_text((directory / 'release-notes.md').read_text() +
                    f'\n自动构建预发布：{tag}。真机及长期稳定性验收仍在进行。\n\n'
                    f'构建记录：https://github.com/{repo}/actions/runs/{run_id}\n')
    # Creating a fresh ref fails closed on collision, including an existing draft.
    gh('api', f'repos/{repo}/git/refs', '--method', 'POST',
       '-f', f'ref=refs/tags/{tag}', '-f', f'sha={source}')
    gh('release', 'create', tag, '--repo', repo, '--verify-tag', '--draft', '--prerelease',
       '--title', f'QuotaPal {version_tag[1:]} · build {run_id}.{attempt}', '--notes-file', str(notes))
    gh('release', 'upload', tag, '--repo', repo, str(apk),
       str(directory / 'SHA256SUMS'), str(directory / 'provenance.json'))
    gh('release', 'edit', tag, '--repo', repo, '--draft=false', '--prerelease', '--latest=false')
    return f'https://github.com/{repo}/releases/tag/{tag}'


if __name__ == '__main__':
    if os.environ.get('GITHUB_EVENT_NAME') != 'push' or os.environ.get('GITHUB_REF') != 'refs/heads/master':
        raise SystemExit('Automatic publication requires a master push')
    url = publish('artifacts/release', os.environ['GITHUB_SHA'], os.environ['GITHUB_RUN_ID'],
                  os.environ['GITHUB_RUN_ATTEMPT'], os.environ['GITHUB_REPOSITORY'])
    print(url)
    with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as summary:
        summary.write(f'\n[Download APK from GitHub Releases]({url})\n')
