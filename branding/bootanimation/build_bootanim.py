#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
부트애니메이션 빌더 — 소스 mp4에서 프레임 추출 → 워터마크 제거 → 업스케일 →
팔레트 양자화 → 핑퐁 루프 → Stored zip 패키징.

사용 전제:
  - ffmpeg 로 프레임을 먼저 추출:  ffmpeg -i source.mp4 frames/f%03d.png
  - Pillow 설치 (pip install pillow)

MGM 고양이 컨셉 기준값 (source_mgm_cat.mp4, 1280x720 24fps 64프레임):
  - 앞부분만: 프레임 1~56 사용 (57~64 = 액자 morph 장면 제외)
  - 워터마크(Gemini sparkle) 제거: 1280x720 기준 x1118-1201, y558-641 검정 마스킹
  - 1920x1080 업스케일(LANCZOS) + 256색 팔레트 + 핑퐁(1..56..2) = 110프레임
  - desc: "1920 1080 24" / "p 0 0 part0"  (무한 루프)
"""
from PIL import Image, ImageDraw
import os, zipfile

SRC   = r"frames"                      # ffmpeg 추출 프레임 폴더 (f001.png ...)
OUT   = r"out"                         # 결과 폴더
FIRST, LAST = 1, 56                    # 사용할 프레임 범위 (앞부분만)
MASK  = (1118, 558, 1201, 641)         # 워터마크 마스크 (소스 해상도 기준), None 이면 생략
W, H  = 1920, 1080                     # 목표 해상도 (패널 = 1080p)
FPS   = 24
COLORS = 256                           # 팔레트 색수 (저사양 rk322x는 128 권장)

def build():
    part0 = os.path.join(OUT, "part0")
    os.makedirs(part0, exist_ok=True)
    for f in os.listdir(part0):
        os.remove(os.path.join(part0, f))
    # 핑퐁: 앞으로 FIRST..LAST, 뒤로 LAST-1..FIRST+1 (하품 열림→닫힘 매끄러운 루프)
    order = list(range(FIRST, LAST + 1)) + list(range(LAST - 1, FIRST, -1))
    for idx, n in enumerate(order):
        im = Image.open(os.path.join(SRC, f"f{n:03d}.png")).convert("RGB")
        if MASK:
            ImageDraw.Draw(im).rectangle(MASK, fill=(0, 0, 0))
        im = im.resize((W, H), Image.LANCZOS).convert(
            "P", palette=Image.ADAPTIVE, colors=COLORS)
        im.save(os.path.join(part0, f"{idx:04d}.png"))
    with open(os.path.join(OUT, "desc.txt"), "w", newline="\n") as f:
        f.write(f"{W} {H} {FPS}\np 0 0 part0\n")     # 마지막 개행 필수
    zp = os.path.join(OUT, "bootanimation.zip")
    if os.path.exists(zp):
        os.remove(zp)
    with zipfile.ZipFile(zp, "w", zipfile.ZIP_STORED) as z:   # 반드시 무압축(Stored)
        z.write(os.path.join(OUT, "desc.txt"), "desc.txt")
        for i in range(len(order)):
            z.write(os.path.join(part0, f"{i:04d}.png"), f"part0/{i:04d}.png")
    print("frames:", len(order), "  zip MB:", round(os.path.getsize(zp) / 1e6, 1))

if __name__ == "__main__":
    build()
