const sqlite3 = require('sqlite3').verbose();
const db = new sqlite3.Database('prisma/dev.db');

// 테이블 목록
db.all("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name", (err, tables) => {
  if (err) { console.error(err.message); db.close(); return; }
  console.log('\n=== 테이블 목록 ===');
  tables.forEach(t => console.log(' -', t.name));

  const deviceTable = tables.find(t => t.name.toLowerCase() === 'device')?.name;
  if (!deviceTable) { console.log('\n기기 테이블 없음'); db.close(); return; }

  // 기기 현황 (appVersion = 현재 설치 버전)
  db.all(`
    SELECT name, ip, appVersion, status,
           datetime(lastSeen,'localtime') as lastSeen,
           datetime(createdAt,'localtime') as createdAt
    FROM "${deviceTable}" ORDER BY name
  `, (e2, rows) => {
    if (e2) { console.error(e2.message); db.close(); return; }
    console.log('\n=== 기기별 앱 버전 & 접속 현황 ===');
    console.table(rows);
    db.close();
  });
});
