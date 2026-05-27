const { Service } = require('node-windows');
const path = require('path');

const svc = new Service({
  name: 'SignagePro Server',
  description: '오설록 SignagePro 원격 관리 서버',
  script: path.join(__dirname, 'index.js'),
  nodeOptions: [],
  workingDirectory: __dirname,
  allowServiceLogon: true,
});

svc.on('install', () => {
  console.log('✅ 서비스 설치 완료! 자동 시작됩니다.');
  svc.start();
});

svc.on('alreadyinstalled', () => {
  console.log('ℹ️  이미 설치된 서비스입니다.');
});

svc.on('error', (err) => {
  console.error('❌ 에러:', err);
});

svc.install();
