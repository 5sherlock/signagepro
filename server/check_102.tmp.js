const { PrismaClient } = require('@prisma/client');
const p = new PrismaClient();

(async () => {
  // 모든 기기
  const devices = await p.device.findMany({ include: { group: true, store: true } });
  console.log('=== 전체 기기 목록 ===');
  devices.forEach(d => {
    console.log(`  [${d.id}] ${d.name} | store=${d.store?.name||'미배정'} | group=${d.group?.name||'미배정'} | status=${d.status}`);
  });

  // 102 기기 찾기
  const dev102 = devices.find(d => d.name.includes('102') || d.id.includes('102'));
  if (dev102 && dev102.groupId) {
    console.log(`\n=== ${dev102.name}의 재생목록 (groupId: ${dev102.groupId}) ===`);
    const playlist = await p.playlist.findFirst({
      where: { groupId: dev102.groupId },
      include: { medias: { include: { media: true }, orderBy: { order: 'asc' } } }
    });
    if (playlist) {
      playlist.medias.forEach(pm => {
        console.log(`  [${pm.order}] ${pm.media.filename} | type=${pm.media.type} | path=${pm.media.path} | target=${pm.targetDeviceId || 'ALL'}`);
      });
    } else {
      console.log('  재생목록 없음');
    }
  } else {
    console.log('\n102 기기를 찾을 수 없거나 그룹 미배정');
  }

  await p.$disconnect();
})();
