const { PrismaClient } = require('@prisma/client');
const fs = require('fs');
const path = require('path');
const prisma = new PrismaClient();

const uploadDir = process.env.UPLOADS_DIR || path.join(__dirname, 'uploads');

async function main() {
  const medias = await prisma.media.findMany();
  const broken = medias.filter(
    m => !fs.existsSync(path.join(uploadDir, path.basename(m.path)))
  );
  const brokenIds = broken.map(m => m.id);

  // 1. 타임라인 연결(PlaylistMedia) 먼저 삭제 (외래키 제약)
  const pmDel = await prisma.playlistMedia.deleteMany({
    where: { mediaId: { in: brokenIds } }
  });
  // 2. 미디어 레코드 삭제
  const mDel = await prisma.media.deleteMany({
    where: { id: { in: brokenIds } }
  });

  console.log(`[DB] PlaylistMedia 삭제: ${pmDel.count}건`);
  console.log(`[DB] Media 삭제: ${mDel.count}건`);

  // 3. uploads 고아 파일 삭제 (DB가 참조하지 않는 파일)
  const remaining = await prisma.media.findMany();
  const referenced = new Set(remaining.map(m => path.basename(m.path)));
  const files = fs.existsSync(uploadDir) ? fs.readdirSync(uploadDir) : [];
  let orphan = 0;
  for (const f of files) {
    const full = path.join(uploadDir, f);
    if (fs.statSync(full).isFile() && !referenced.has(f)) {
      fs.unlinkSync(full);
      orphan++;
    }
  }
  console.log(`[FS] uploads 고아 파일 삭제: ${orphan}개`);

  // 결과 확인
  const finalMedia = await prisma.media.count();
  const finalPm = await prisma.playlistMedia.count();
  console.log(`\n[결과] 남은 Media: ${finalMedia}개 | 남은 PlaylistMedia: ${finalPm}건`);
}

main().catch(console.error).finally(() => prisma.$disconnect());
