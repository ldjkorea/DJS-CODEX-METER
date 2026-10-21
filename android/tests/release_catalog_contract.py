"""Offline archive contract: keep every published entry; permit reviewed new versions."""
from pathlib import Path
import json
import re
import subprocess

PUBLISHED_ARCHIVE = '6638a50b3771d717bad9c6704f6c9924bda418fa'
ARCHIVE_PATH = 'android/app/src/main/assets/release-history.json'


def formal_changelog_versions(text):
    # Explicit BETA headings belong to the separate beta package, not this stable archive.
    return {version for version, suffix in re.findall(r'^#{1,2} (\d+\.\d+\.\d+)([^\n]*)', text, re.M)
            if not re.search(r'\bBETA\b|베타', suffix, re.I)}


def validate_catalog(rows, published, current_version):
    assert isinstance(rows, list) and rows, 'Release archive must be nonempty'
    versions = [row['version'] for row in rows]
    assert all(re.fullmatch(r'\d+\.\d+\.\d+', version) for version in versions), 'Invalid archive version'
    assert len(set(versions)) == len(versions), 'Duplicate archive version'
    ordered = sorted(versions, key=lambda version: tuple(map(int, version.split('.'))), reverse=True)
    assert versions == ordered, 'Archive order must be newest to oldest'
    assert versions[0] == current_version, 'Archive newest entry must match the app version'
    by_version = {row['version']: row for row in rows}
    for old in published:
        assert by_version.get(old['version']) == old, ('Published history removed or changed', old['version'])
    for row in rows:
        for language in ['ko', 'en']:
            localized = row[language]
            assert localized['title'].strip(), ('Missing title', row['version'], language)
            assert localized['changes'] and all(isinstance(line, str) and line.strip() for line in localized['changes']), ('Missing notes', row['version'], language)
    return versions


def verify_catalog(repo):
    repo = Path(repo)
    published = json.loads(subprocess.check_output(['git', 'show', PUBLISHED_ARCHIVE + ':' + ARCHIVE_PATH], cwd=repo))
    current = json.loads((repo / ARCHIVE_PATH).read_text(encoding='utf-8'))
    constants = (repo / 'android/app/src/main/java/dev/bennett/codexmeter/AppConstants.java').read_text(encoding='utf-8')
    version = re.search(r'VERSION_NAME = "([^"]+)";', constants).group(1)
    versions = validate_catalog(current, published, version)
    historic = formal_changelog_versions((repo / 'CHANGELOG.md').read_text(encoding='utf-8'))
    assert historic.issubset(versions), 'Changelog history missing from offline catalog'
    return current, versions
