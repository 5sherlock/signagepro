const { PrismaClient } = require('@prisma/client');
const p = new PrismaClient();
p.media.findMany({ select: { filename: true, hash: true, path: true } })
  .then(ms => {
    let missing = 0;
    ms.forEach(m => {
      const status = m.hash ? '✅' : '❌ hash없음';
      console.log(`${status} ${m.filename} | hash: ${m.hash ?? '(null)'}`);
      if (!m.hash) missing++;
    });
    console.log(`\n총 ${ms.length}개 중 hash 없음: ${missing}개`);
  })
  .finally(() => p.$disconnect());
