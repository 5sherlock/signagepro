const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function main() {
  let store = await prisma.store.findFirst({ where: { name: '오설록' } });
  if (!store) {
    store = await prisma.store.create({ data: { name: '오설록' } });
  }

  const updatedGroups = await prisma.group.updateMany({
    where: { storeId: null },
    data: { storeId: store.id }
  });

  const updatedDevices = await prisma.device.updateMany({
    where: { storeId: null },
    data: { storeId: store.id }
  });

  console.log(`Successfully assigned ${updatedDevices.count} devices and ${updatedGroups.count} groups to '오설록'.`);
}

main().catch(console.error).finally(() => prisma.$disconnect());
