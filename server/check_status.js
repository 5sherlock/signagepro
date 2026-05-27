const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function main() {
  const devices = await prisma.device.findMany({
    include: {
      store: true,
      group: true
    }
  });

  console.log('=== DEVICE STATUS IN DATABASE ===');
  if (devices.length === 0) {
    console.log('No devices found.');
  } else {
    devices.forEach(d => {
      console.log(`Device ID: ${d.id}`);
      console.log(`  Name: ${d.name}`);
      console.log(`  Status: ${d.status}`);
      console.log(`  IP: ${d.ip || 'N/A'}`);
      console.log(`  Last Seen: ${d.lastSeen ? d.lastSeen.toISOString() : 'Never'}`);
      console.log(`  CPU Usage: ${d.cpuUsage !== null ? d.cpuUsage + '%' : 'N/A'}`);
      console.log(`  Memory Usage: ${d.memUsage !== null ? d.memUsage + '%' : 'N/A'}`);
      console.log(`  App Version: ${d.appVersion || 'N/A'}`);
      console.log('---------------------------------');
    });
  }
}

main()
  .catch(e => console.error(e))
  .finally(async () => await prisma.$disconnect());
