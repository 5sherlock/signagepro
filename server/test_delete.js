const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function main() {
  const deleted = await prisma.device.delete({
    where: { id: 'ANDROID-62T5R6' }
  });
  console.log('Successfully deleted:', deleted);
}

main()
  .catch(e => console.error('Deletion error:', e))
  .finally(async () => await prisma.$disconnect());
