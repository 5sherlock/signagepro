#!/usr/bin/env node
/**
 * R2(공용 클라우드) → 로컬 /uploads 미디어 마이그레이션.
 *
 * 동작: Media.path 가 http(s) (= R2 URL)인 항목을 R2에서 내려받아
 *       server/uploads/<파일명> 으로 저장하고, path 를 /uploads/<파일명> 으로 갱신한다.
 *  - 기본은 DRY-RUN(미적용). 실제 적용은 --apply.
 *  - 멱등: 파일이 이미 있으면 재다운로드 생략, path만 갱신. 재실행 안전.
 *  - 무결성: media.hash(SHA-256)가 있으면 받은 파일과 대조, 불일치 시 실패 처리.
 *
 * 사용:
 *   node scripts/migrate_r2_to_local.js              # DRY-RUN (무엇을 바꿀지 미리보기)
 *   node scripts/migrate_r2_to_local.js --apply      # 실제 적용
 *
 * ※ 반드시 해당 환경의 server 폴더에서 실행 (그 환경의 .env / DATABASE_URL / uploads 사용).
 *   dev:     D:\WorkSpace\signagepro\server
 *   staging: D:\signagepro\server
 *   운영/현장: AnyDesk로 현장 서버 server 폴더에서 실행
 */
require('dotenv').config();
const { PrismaClient } = require('@prisma/client');
const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');
const crypto = require('crypto');

const APPLY = process.argv.includes('--apply');
const prisma = new PrismaClient();
const uploadsDir = process.env.UPLOADS_DIR || path.join(__dirname, '..', 'uploads');

function download(url) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith('https') ? https : http;
    lib.get(url, (res) => {
      if (res.statusCode !== 200) { res.resume(); return reject(new Error('HTTP ' + res.statusCode)); }
      const chunks = [];
      res.on('data', (c) => chunks.push(c));
      res.on('end', () => resolve(Buffer.concat(chunks)));
    }).on('error', reject);
  });
}

const sha256 = (buf) => crypto.createHash('sha256').update(buf).digest('hex');

(async () => {
  if (APPLY && !fs.existsSync(uploadsDir)) fs.mkdirSync(uploadsDir, { recursive: true });

  const all = await prisma.media.findMany();
  const r2 = all.filter((m) => m.path && /^https?:\/\//i.test(m.path));
  console.log(`총 Media ${all.length}개 / R2 경로 ${r2.length}개  |  모드: ${APPLY ? 'APPLY(실제 적용)' : 'DRY-RUN(미적용)'}`);
  console.log(`uploads: ${uploadsDir}\n`);
  if (r2.length === 0) { console.log('R2 경로 미디어 없음 — 마이그레이션 불필요.'); return; }

  let ok = 0, fail = 0, reused = 0, bytes = 0;
  for (const m of r2) {
    const base = decodeURIComponent(m.path.split('/').pop().split('?')[0]);
    const dest = path.join(uploadsDir, base);
    const newPath = `/uploads/${base}`;
    const exists = fs.existsSync(dest) && fs.statSync(dest).size > 0;

    if (!APPLY) {
      console.log(`  [dry] ${m.filename}\n        ${m.path}\n        -> ${newPath}${exists ? '  (로컬 파일 이미 있음)' : ''}`);
      continue;
    }
    try {
      if (!exists) {
        const buf = await download(m.path);
        if (m.hash) {
          const h = sha256(buf);
          if (h.toLowerCase() !== m.hash.toLowerCase())
            throw new Error(`해시 불일치 (DB ${m.hash.slice(0, 12)}… vs 받음 ${h.slice(0, 12)}…)`);
        }
        fs.writeFileSync(dest, buf);
        bytes += buf.length;
      } else {
        reused++;
      }
      await prisma.media.update({ where: { id: m.id }, data: { path: newPath } });
      console.log(`  [ok] ${m.filename} -> ${newPath}${exists ? ' (기존 파일 사용)' : ''}`);
      ok++;
    } catch (e) {
      console.log(`  [FAIL] ${m.filename}  ${m.path}\n         ${e.message}`);
      fail++;
    }
  }

  console.log(`\n완료: 성공 ${ok}, 실패 ${fail}, 기존재사용 ${reused}, 다운로드 ${(bytes / 1048576).toFixed(1)}MB`);
  if (!APPLY) console.log('\n※ DRY-RUN이었습니다. 실제 적용하려면:  node scripts/migrate_r2_to_local.js --apply');
  if (APPLY && fail === 0) console.log('\n다음: 이 환경 .env의 R2_* 를 주석 처리해 이후 업로드도 로컬로 가게 하세요(서버 재시작).');
})().catch((e) => { console.error(e); process.exit(1); }).finally(() => prisma.$disconnect());
