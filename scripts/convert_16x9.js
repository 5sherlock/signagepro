const sharp = require('sharp');
const path = require('path');
const fs = require('fs');

const dir = path.join(__dirname, '..', 'sample_media');
const TARGET_W = 1920;
const TARGET_H = 1080; // 16:9

async function convertTo16x9(file) {
  const input = path.join(dir, file);
  const tmp   = path.join(dir, '__tmp_' + file);

  const meta = await sharp(input).metadata();
  console.log(`[${file}] 원본: ${meta.width}x${meta.height}`);

  await sharp(input)
    .resize(TARGET_W, TARGET_H, {
      fit: 'cover',
      position: 'centre',
    })
    .png({ quality: 95 })
    .toFile(tmp);

  // 임시 파일을 원본 파일명으로 교체
  fs.renameSync(tmp, input);
  console.log(`[${file}] → ${TARGET_W}x${TARGET_H} 완료`);
}

async function main() {
  const files = fs.readdirSync(dir).filter(f => /\.(png|jpg|jpeg|webp)$/i.test(f));
  console.log(`총 ${files.length}개 파일 변환 시작...\n`);
  for (const f of files) {
    await convertTo16x9(f);
  }
  console.log('\n✅ 모든 파일 변환 완료!');
}

main().catch(console.error);
