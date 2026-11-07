#!/usr/bin/env python3
"""Build a CurseForge-importable profile zip: Minecraft + Fabric + Fabric API + SvenskaCraft.

Usage: python3 tools/make_curseforge_pack.py <fabric-api.jar>
Reads versions from gradle.properties; writes dist/SvenskaCraft-CurseForge-<version>.zip.
In the CurseForge app: Minecraft → Create Custom Profile → Import → pick the zip.
"""
import json
import pathlib
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
props = dict(l.split("=", 1) for l in (ROOT / "gradle.properties").read_text().splitlines() if "=" in l and not l.startswith("#"))
version = props["version"].strip()
mc = props["minecraft_version"].strip()
loader = props["loader_version"].strip()
mod_jar = ROOT / "build/libs" / f"svenskacraft-{version}.jar"
api_jar = pathlib.Path(sys.argv[1])

manifest = {
    "minecraft": {"version": mc, "modLoaders": [{"id": f"fabric-{loader}", "primary": True}]},
    "manifestType": "minecraftModpack",
    "manifestVersion": 1,
    "name": "SvenskaCraft",
    "version": version,
    "author": "andreaskannaos-pixel",
    "files": [],
    "overrides": "overrides",
}
modlist = f"""<ul>
<li>SvenskaCraft {version} – learn Swedish while you play</li>
<li>Fabric API {api_jar.stem.removeprefix('fabric-api-')} (Apache-2.0, bundled unchanged)</li>
</ul>
"""
out = ROOT / "dist" / f"SvenskaCraft-CurseForge-{version}.zip"
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("manifest.json", json.dumps(manifest, indent=2))
    z.writestr("modlist.html", modlist)
    z.write(mod_jar, f"overrides/mods/{mod_jar.name}")
    z.write(api_jar, f"overrides/mods/{api_jar.name}")
print(out)
