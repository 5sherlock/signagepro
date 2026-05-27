const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function main() {
  const playlist = await prisma.playlist.findFirst({
    where: { group: { name: '매장' } },
    include: {
      medias: {
        include: { media: true },
        orderBy: { order: 'asc' }
      }
    }
  });

  if (!playlist) { console.log('플레이리스트 없음'); return; }

  console.log(`플레이리스트: ${playlist.name}`);
  console.log(`총 미디어: ${playlist.medias.length}개\n`);

  playlist.medias.forEach(pm => {
    console.log(`[order:${pm.order}] ${pm.media.filename}`);
    console.log(`  targetDeviceId: ${pm.targetDeviceId ?? '(null = 전체)'}`);
  });

  // 각 기기별 받을 수 있는 아이템 수
  const devices = await prisma.device.findMany({ where: { group: { name: '매장' } } });
  console.log('\n=== 기기별 필터 결과 ===');
  devices.forEach(dev => {
    const visible = playlist.medias.filter(pm => !pm.targetDeviceId || pm.targetDeviceId === dev.id);
    console.log(`${dev.name} (id: ${dev.id}): ${visible.length}개 보임`);
    visible.forEach(pm => console.log(`  - ${pm.media.filename}`));
  });
}

main().catch(console.error).finally(() => prisma.$disconnect());
