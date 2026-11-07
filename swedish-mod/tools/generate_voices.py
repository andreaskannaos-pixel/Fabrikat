#!/usr/bin/env python3
"""Generate voice clips + sounds.json for one SvenskaCraft language pack.

Needs: pip install piper-tts, ffmpeg, and a Piper voice for the language.
Swedish uses sv-se-nst-medium (CC0):
https://github.com/rhasspy/piper/releases/download/v0.0.2/voice-sv-se-nst-medium.tar.gz

Usage: python3 tools/generate_voices.py <lang> <voice.onnx>
   eg: python3 tools/generate_voices.py sv sv-se-nst-medium.onnx

Clips: sounds/<lang>/word/<id>.ogg, sounds/<lang>/example/<id>.ogg,
       sounds/<lang>/react/<kind>_<n>.ogg
"""
import json
import pathlib
import shutil
import subprocess
import sys
import tempfile
import wave

from piper import PiperVoice, SynthesisConfig

ROOT = pathlib.Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/svenskacraft"

# Trim silence at both ends, even out loudness, soft fade-in, short tail.
FILTER = ("silenceremove=start_periods=1:start_threshold=-45dB,areverse,"
          "silenceremove=start_periods=1:start_threshold=-45dB,areverse,"
          "loudnorm=I=-18:TP=-2:LRA=7,afade=t=in:d=0.02,apad=pad_dur=0.12")


def clips_for(lang: str) -> dict[str, str]:
    words = json.loads((ASSETS / "languages" / lang / "words.json").read_text("utf-8"))
    pack = json.loads((ASSETS / "languages" / lang / "pack.json").read_text("utf-8"))
    clips = {}
    for cid, w in words.items():
        word = w["word"]
        clips[f"word/{cid}"] = word if word[-1] in ".!?" else word + "."
        clips[f"example/{cid}"] = w["example"]
    for kind, lines in pack["reactions"].items():
        for i, line in enumerate(lines, 1):
            clips[f"react/{kind}_{i}"] = line
    return clips


def main(lang: str, model: str) -> None:
    voice = PiperVoice.load(model)
    words_cfg = SynthesisConfig(length_scale=1.12)   # slightly slower for single words
    speech_cfg = SynthesisConfig(length_scale=1.0)
    out_root = ASSETS / "sounds" / lang
    shutil.rmtree(out_root, ignore_errors=True)
    clips = clips_for(lang)
    sounds_file = ASSETS / "sounds.json"
    sounds = json.loads(sounds_file.read_text("utf-8")) if sounds_file.exists() else {}
    sounds = {k: v for k, v in sounds.items() if not k.startswith(lang + ".")}
    with tempfile.TemporaryDirectory() as tmp:
        wav = pathlib.Path(tmp) / "a.wav"
        for name, text in clips.items():
            out = out_root / f"{name}.ogg"
            out.parent.mkdir(parents=True, exist_ok=True)
            with wave.open(str(wav), "wb") as f:
                voice.synthesize_wav(text, f, syn_config=words_cfg if name.startswith("word/") else speech_cfg)
            subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", str(wav), "-af", FILTER,
                            "-ar", "22050", "-ac", "1", "-c:a", "libvorbis", "-q:a", "4", str(out)], check=True)
            sounds[f"{lang}.{name.replace('/', '.')}"] = {"sounds": [{"name": f"svenskacraft:{lang}/{name}"}]}
    sounds_file.write_text(json.dumps(dict(sorted(sounds.items())), indent=1, ensure_ascii=False), "utf-8")
    print(f"{len(clips)} clips written for '{lang}'")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
