const { PrismaClient } = require('@prisma/client');
const fs = require('fs');
const path = require('path');
const prisma = new PrismaClient();

const uploadDir = process.env.UPLOADS_DIR || path.join(__dirname, 'uploads');

async function main() {
  const medias = await prisma.media.findMany({
    include: { _count: { select: { playlists: true } } }
  });

  const broken = [];
  const ok = [];
  for (const m of medias) {
    const fileName = path.basename(m.path);
    const fullPath = path.join(uploadDir, fileName);
    const exists = fs.existsSync(fullPath);
    (exists ? ok : broken).push(m);
  }

  console.log(`총 미디어: ${medias.length}개 | 파일 있음: ${ok.length} | 파일 없음(깨짐): ${broken.length}\n`);

  console.log('=== 깨진 미디어 (파일 없음) ===');
  broken.forEach(m => {
    console.log(`  ${m.filename}  | path=${m.path} | hash=${m.hash ? '있음' : 'null'} | 타임라인 연결=${m._count.playlists}건`);
  });

  console.log('\n=== 정상 미디어 (파일 있음) ===');
  ok.forEach(m => {
    console.log(`  ${m.filename}  | path=${m.path} | hash=${m.hash ? '있음' : 'null'}`);
  });
}

main().catch(console.error).finally(() => prisma.$disconnect());
