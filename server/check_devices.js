const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function main() {
  const devices = await prisma.device.findMany({
    include: {
      store: true,
      group: true
    }
  });

  console.log('--- Device Assignments ---');
  if (devices.length === 0) {
    console.log('No devices found in the database.');
  } else {
    devices.forEach(d => {
      console.log(`Device ID: ${d.id}`);
      console.log(`  Name: ${d.name}`);
      console.log(`  Store: ${d.store ? d.store.name : 'Unassigned'}`);
      console.log(`  Group: ${d.group ? d.group.name : 'Unassigned'}`);
      console.log('-------------------------');
    });
  }

  const stores = await prisma.store.findMany();
  console.log('--- Stores List ---');
  stores.forEach(s => console.log(`Store ID: ${s.id}, Name: ${s.name}`));
}

main()
  .catch(e => console.error(e))
  .finally(async () => await prisma.$disconnect());
