const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();
async function main() {
  await prisma.device.updateMany({
    where: { status: 'offline' },
    data: { status: 'online' }
  });
  console.log('Set all devices to online.');
}
main().catch(console.error).finally(() => prisma.$disconnect());
