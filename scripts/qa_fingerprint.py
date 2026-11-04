"""Deterministic code/schema fingerprints for a tested, possibly uncommitted tree."""
import hashlib

def source_fingerprints(base):
    result={}
    for folder in ['src/main','src/test','database']:
        digest=hashlib.sha256()
        for path in sorted((base/folder).rglob('*')):
            if path.is_file() and '__pycache__' not in path.parts:
                digest.update(path.relative_to(base).as_posix().encode()+b'\0'+path.read_bytes()+b'\0')
        result[folder]=digest.hexdigest()
    for name in ['pom.xml','schema_postgres.sql','scripts/test-room-absence-postgres.py','scripts/test-phases-postgres.py','scripts/prepare-phases-release.py']:
        result[name]=hashlib.sha256((base/name).read_bytes()).hexdigest()
    return result
