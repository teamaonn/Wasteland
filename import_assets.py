#!/usr/bin/env python3
"""Import verified assets from an existing personal Wasteland build; never fetch them."""
import argparse, hashlib, json, zipfile
from pathlib import Path, PurePosixPath

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('jar',type=Path)
    parser.add_argument('--destination',type=Path,default=Path(__file__).resolve().parent)
    args=parser.parse_args()
    manifest=json.loads(Path(__file__).with_name('asset-manifest.json').read_text())
    pending=[]
    with zipfile.ZipFile(args.jar) as jar:
        for entry in manifest['files']:
            relative=PurePosixPath(entry['path'])
            if relative.is_absolute() or '..' in relative.parts or not str(relative).startswith('src/main/resources/assets/wasteland/'):
                raise SystemExit('Unsafe asset path in manifest')
            try: data=jar.read(entry['jar_path'])
            except KeyError: raise SystemExit('Missing required asset: '+entry['jar_path'])
            if len(data)!=entry['size'] or hashlib.sha256(data).hexdigest()!=entry['sha256']:
                raise SystemExit('Asset differs from the tested 0.2.3 build: '+entry['jar_path'])
            pending.append((args.destination.joinpath(*relative.parts),data))
    for target,data in pending:
        target.parent.mkdir(parents=True,exist_ok=True)
        target.write_bytes(data)
    print(f'Imported {len(pending)} verified assets locally. Original asset rights remain unchanged.')

if __name__=='__main__': main()
