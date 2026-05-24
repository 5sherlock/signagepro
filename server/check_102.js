const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function main() {
  const devices = await prisma.device.findMany({
    include: {
      group: {
        include: {
          playlists: {
            include: { medias: { include: { media: true } } }
          }
        }
      }
    }
  });

  const dev102 = devices.find(d => d.name && d.name.includes('102'));

  if (dev102) {
    console.log('=== 102 기기 ===');
    console.log('Name:', dev102.name);
    console.log('Status:', dev102.status);
    console.log('GroupId:', dev102.groupId);
    if (dev102.group) {
      console.log('Group:', dev102.group.name);
      console.log('Playlists count:', dev102.group.playlists.length);
      dev102.group.playlists.forEach(pl => {
        console.log('  Playlist:', pl.name, '- Media count:', pl.medias.length);
        pl.medias.forEach(m => {
          console.log('    -', m.media ? m.media.filename : '(null)');
        });
      });
    } else {
      console.log('Group: 없음 (그룹 미할당)');
    }
  } else {
    console.log('102 기기 없음. 전체 기기 목록:');
    devices.forEach(d => console.log(' -', d.name, '|', d.status, '| groupId:', d.groupId));
  }

  const medias = await prisma.media.findMany();
  console.log('\n=== 전체 미디어 ===');
  if (medias.length === 0) console.log('  (없음)');
  medias.forEach(m => console.log(' -', m.filename, '|', m.type));

  const playlists = await prisma.playlist.findMany({ include: { medias: true } });
  console.log('\n=== 전체 플레이리스트 ===');
  if (playlists.length === 0) console.log('  (없음)');
  playlists.forEach(pl => console.log(' -', pl.name, '| 미디어', pl.medias.length, '개 | groupId:', pl.groupId));
}

main().catch(console.error).finally(() => prisma.$disconnect());
