const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function main() {
  const stores = await prisma.store.findMany();
  console.log('=== STORES AND GROUPS ===');
  for (const s of stores) {
    console.log(`Store: ${s.name} (ID: ${s.id})`);
    const groups = await prisma.group.findMany({ where: { storeId: s.id } });
    for (const g of groups) {
      console.log(`  Group: ${g.name} (ID: ${g.id})`);
      const playlist = await prisma.playlist.findFirst({
        where: { groupId: g.id },
        include: {
          medias: {
            include: { media: true },
            orderBy: { order: 'asc' }
          }
        }
      });
      if (playlist) {
        console.log(`    Playlist: ${playlist.name} (ID: ${playlist.id})`);
        playlist.medias.forEach(m => {
          console.log(`      - Media: ${m.media.filename} (Duration: ${m.duration}s, Path: ${m.media.path}, Target: ${m.targetDeviceId || 'All'})`);
        });
      } else {
        console.log(`    Playlist: None`);
      }
    }
  }

  const allMedia = await prisma.media.findMany();
  console.log('\n=== ALL MEDIA FILES IN DB ===');
  allMedia.forEach(m => {
    console.log(`  - [${m.type}] ${m.filename} (Size: ${(m.size / 1024 / 1024).toFixed(2)}MB, Path: ${m.path})`);
  });
}

main()
  .catch(e => console.error(e))
  .finally(async () => await prisma.$disconnect());
