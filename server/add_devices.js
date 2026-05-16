const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();
async function main() {
  const store = await prisma.store.findFirst({ where: { name: '오설록' } });
  let group = await prisma.group.findFirst({ where: { storeId: store.id, name: '매장' } });
  
  if (!store) {
    console.log('Store not found.');
    return;
  }
  if (!group) {
    group = await prisma.group.findFirst({ where: { storeId: store.id } });
  }

  await prisma.device.create({
    data: {
      id: 'Device-dev-104',
      name: 'Device-dev-104',
      storeId: store.id,
      groupId: group.id,
      ip: '192.168.1.104',
      status: 'online'
    }
  });

  await prisma.device.create({
    data: {
      id: 'Device-dev-105',
      name: 'Device-dev-105',
      storeId: store.id,
      groupId: group.id,
      ip: '192.168.1.105',
      status: 'offline'
    }
  });

  console.log('Added Device-dev-104 and Device-dev-105 to 오설록 > ' + group.name);
}
main().catch(console.error).finally(() => prisma.$disconnect());
