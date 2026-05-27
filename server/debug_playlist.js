const sqlite3 = require('sqlite3').verbose();
const db = new sqlite3.Database('prisma/dev.db');

db.all("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name", (err, tables) => {
  if (err) { console.error(err.message); db.close(); return; }
  console.log('Tables:', tables.map(t => t.name).join(', '));

  // PlaylistMedia 구조
  db.all("PRAGMA table_info(PlaylistMedia)", (e2, cols) => {
    if (!e2) {
      console.log('\nPlaylistMedia columns:', cols.map(c => c.name).join(', '));
    }

    // PlaylistMedia 전체 데이터
    db.all("SELECT * FROM PlaylistMedia ORDER BY \"order\"", (e3, rows) => {
      if (e3) { console.error(e3.message); }
      else {
        console.log('\nPlaylistMedia rows:');
        console.table(rows);
      }

      // Media 전체 데이터
      db.all("SELECT id, filename, type FROM Media", (e4, medias) => {
        if (e4) { console.error(e4.message); }
        else {
          console.log('\nMedia:');
          console.table(medias);
        }
        db.close();
      });
    });
  });
});
