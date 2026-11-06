#!/usr/bin/env python3
"""Generate Swedish voice clips + sounds.json for SvenskaCraft.

Needs: pip install piper-tts, ffmpeg, and the Piper voice sv-se-nst-medium
(https://github.com/rhasspy/piper/releases/download/v0.0.2/voice-sv-se-nst-medium.tar.gz).

Usage: python3 tools/generate_voices.py /path/to/sv-se-nst-medium.onnx
"""
import json
import pathlib
import subprocess
import sys
import tempfile
import wave

from piper import PiperVoice, SynthesisConfig

ROOT = pathlib.Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/svenskacraft"

FX = {
    "quiz": "Dags för quiz!",
    "right": "Rätt! Bra jobbat!",
    "timeout": "Tiden är ute!",
}


def main(model: str) -> None:
    voice = PiperVoice.load(model)
    cfg = SynthesisConfig(length_scale=1.15)
    words = json.loads((ASSETS / "words.json").read_text("utf-8"))
    clips = {f"fx/{k}": v for k, v in FX.items()}
    for w in words:
        clips[f"w/{w['id']}"] = w["sv"] + "."
        clips[f"fb/{w['id']}"] = f"Fel. Rätt svar är: {w['sv']}."

    sounds = {}
    with tempfile.TemporaryDirectory() as tmp:
        for name, text in clips.items():
            out = ASSETS / "sounds" / f"{name}.ogg"
            out.parent.mkdir(parents=True, exist_ok=True)
            wav = pathlib.Path(tmp) / "a.wav"
            with wave.open(str(wav), "wb") as f:
                voice.synthesize_wav(text, f, syn_config=cfg)
            subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", str(wav),
                            "-ac", "1", "-c:a", "libvorbis", "-q:a", "3", str(out)], check=True)
            sounds[name.replace("/", ".")] = {"sounds": [{"name": f"svenskacraft:{name}"}]}
    (ASSETS / "sounds.json").write_text(json.dumps(sounds, indent=1, ensure_ascii=False), "utf-8")
    print(f"{len(clips)} clips written")


if __name__ == "__main__":
    main(sys.argv[1])
