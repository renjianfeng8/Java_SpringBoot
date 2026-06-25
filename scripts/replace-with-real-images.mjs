// ============================================================
// Cinema System - Replace placeholders with real images
// Node.js version (handles UTF-8 filenames correctly)
// ============================================================
import fs from 'fs';
import path from 'path';
import { execSync, spawn } from 'child_process';

const PictureDir = 'D:/project/project_02/picture';
const VideoDir = 'D:/project/project_02/video';
const SeedDir = 'D:/project/project_02/xm_film/sql/seed-uploads';
const SshKey = 'D:/chrome_download/server_ssh_key_2026.pem';
const Server = 'root@rjfwealth.cn';

// ============================================================
// MAPPING: real filename -> UUID (from data.sql)
// ============================================================
const filmPosters = {
  '749局.jpg': '2b8cd257-5ba1-43e3-9756-c920411cfb44.jpg',
  '伟大征程.jpg': '827002f2-3960-4712-a4f6-a7da746e49c6.jpg',
  '危机航线.jpg': '41a5b2c9-14b5-4d4d-9a27-16d3db09320c.jpg',
  '变形金刚：起源.jpg': '51fa4d27-e50f-44df-9dc3-293b930d93f6.jpg',
  '哈利·波特与凤凰社.jpg': '09cabb7f-c4fe-4908-ad11-d134ccdfd566.jpg',
  '哈利·波特与火焰杯.jpg': 'bd88a9d6-b3f9-4fce-aa60-3ae4b745e613.jpg',
  '最后的里程.jpg': '9f01f773-6c3a-4c8d-85c1-ddcb47d74ac4.jpg',
  '暮然回首.jpg': '537c0725-77da-485f-a76a-c3891477640d.jpg',
  '毒液：最后一舞.jpg': 'f6add364-ca35-4596-a1bc-9f6543228b72.jpg',
  '海绵宝宝.jpg': 'dac64520-915f-4b61-95dc-3018c2390ebf.jpg',
  '熊猫计划.jpg': 'b51ad4f5-25fa-4d8e-b13e-1f2ac6c4d3cc.jpg',
  '爱你很久很久.jpg': 'af3145d2-cf73-473f-8690-4450da51690e.jpg',
  '爱情神话.jpg': '5d75701e-567b-4684-a618-a423130e4610.jpg',
  '红色一号：冬日行动.jpg': 'e27d255c-186b-45b5-9dc5-e059f1faf269.jpg',
  '这个杀手不太冷.jpg': 'fc8ed88c-2719-40ed-9f4b-b5efd34ade55.jpg',
  '那个不为人知的故事.jpg': 'e993904e-2e34-4d32-80cc-c6695a00c6b9.jpg',
  '志愿军.jpg': '4253ae24-a226-45bf-b8c4-0fbd9824e3e2.jpg',
};

const cinemaLogos = {
  '万象城影城.jpg': '4e62ebfa-30f3-4e85-9b3f-2526ecac48e3.jpg',
  '万达影城.jpg': '9e32dc92-5a1c-4ed5-bed2-4bef6f652380.jpg',
  '奥斯卡赛影城.jpg': 'e2935134-646b-4a56-b81d-8b5fc07f728e.jpg',
  '丁丁影城.jpg': '06756954-4f38-4374-863a-c740b588e109.jpg',
};

const actorPhotos = {
  '刘德华.jpg': 'ea4343cc-6ba0-4a57-a2aa-bd4775ffae99.jpg',
  '张子枫.jpg': '15612c22-5124-4618-9c37-c489db4fc899.jpg',
  '张子枫2.jpg': '6b8b8db0-0887-4541-a7fe-ccad09fd34c4.jpg',
  '张梓宸.jpg': '2390faa7-14e7-4a80-a5e0-6e90a642a23b.jpg',
  '成龙.jpg': 'cea34574-05ee-4138-984b-d8af9f03bb0a.jpg',
  '朱一龙.jpg': 'd6e7046b-35f0-4524-abb6-723f95ceb8da.jpg',
};

const actorHeadshots = {
  '刘德华.jpg': '9aefa1dd-3a59-4d22-a267-2b51e7aeff98.jpg',
  '张子枫2.jpg': '77bde2f0-39bb-46c9-a274-c1f9870f2d2b.jpg',
  '张梓宸.jpg': 'e5c0bbd6-2269-4bfe-9f9d-15340346a992.jpg',
  '成龙.jpg': '2427debd-1a4d-4821-a658-70ca584358f8.jpg',
  '朱一龙.jpg': '8f324b5b-7fe1-4df9-8f22-8187040feec6.jpg',
  '范欣.jpg': 'e5c0bbd6-2269-4bfe-9f9d-15340346a992.jpg',
};

const userAvatars = {
  'admin.jpg': 'fac908b2-616f-42e2-b880-a0df7b09ce56.jpg',
};

const videoCovers = {
  '熊猫计划.jpg': '4343a492-ed82-4afb-bbad-3c3d864f15ac.jpg',
  '海绵宝宝.jpg': 'f2b47831-9c0b-4863-b2f1-c350d29dcd1e.jpg',
  '毒液：最后一舞.jpg': '907179ba-a444-43e9-89aa-300dba2f8aba.jpg',
  '749局.jpg': '26de5bca-6c38-49dc-ae44-38a188319953.jpg',
  '暮然回首.jpg': '557d41b1-fec3-4f0d-98e1-b5fe6421680f.jpg',
  '变形金刚：起源.jpg': '2bf9d49e-26a8-45c1-804d-f79ee617dda2.jpg',
  '危机航线.jpg': 'feb7370a-0912-44a2-bf85-d4a87f631dd8.jpg',
  '这个杀手不太冷.jpg': 'fcff7c26-ab04-4212-ba0b-03b867aa997b.jpg',
  '志愿军.jpg': 'e9a36369-61e5-4441-bc8d-54b6864d5272.jpg',
};

const videoMp4s = {
  '749.mp4': '1090dbba-fdc5-4836-9f4a-323d250fdc13.mp4',
  '不为人知的故事.mp4': 'f9d7ef46-ae54-43ba-9e4d-565f4613bff7.mp4',
  '危机航线宣传视频.mp4': '8a96e0ce-2f6c-45c9-90a1-9df1bc9b9d01.mp4',
  '变形金刚.mp4': '18ecdf7f-b9d7-4d99-994c-0353cf8774cf.mp4',
  '志愿军.mp4': '12906d8e-e922-4251-99c4-6c9ae942fd72.mp4',
  '毒液.mp4': 'fa10ff2d-e7d5-4a9a-ae6e-d165809bfb50.mp4',
  '海绵宝宝.mp4': '3039214a-7d12-4dc6-b896-d80ecdbd714b.mp4',
  '熊猫计划.mp4': 'f415156f-eee6-4bfe-9a24-a2f7734998cb.mp4',
  '蓦然回首.mp4': '9f0aeaf5-35b7-4da7-802e-741c8d75a075.mp4',
  '这个杀手不太冷.mp4': 'e9207194-9e2f-49e1-94ee-a575d207b551.mp4',
};

// ============================================================
// STEP 1: Copy real files to seed-uploads with UUID names
// ============================================================
const allMappings = { ...filmPosters, ...cinemaLogos, ...actorPhotos, ...actorHeadshots, ...userAvatars, ...videoCovers };

console.log('=== Copying real files to seed-uploads ===');
let copied = 0, missing = [];

for (const [srcName, dstName] of Object.entries(allMappings)) {
  const srcPath = path.join(PictureDir, srcName);
  const dstPath = path.join(SeedDir, dstName);
  if (fs.existsSync(srcPath)) {
    fs.copyFileSync(srcPath, dstPath);
    console.log(`  [OK] ${srcName} -> ${dstName}`);
    copied++;
  } else {
    console.log(`  [??] ${srcName} NOT FOUND`);
    missing.push(srcName);
  }
}

for (const [srcName, dstName] of Object.entries(videoMp4s)) {
  const srcPath = path.join(VideoDir, srcName);
  const dstPath = path.join(SeedDir, dstName);
  if (fs.existsSync(srcPath)) {
    fs.copyFileSync(srcPath, dstPath);
    console.log(`  [OK] ${srcName} -> ${dstName}`);
    copied++;
  } else {
    console.log(`  [??] ${srcName} NOT FOUND`);
    missing.push(srcName);
  }
}

console.log(`\nCopied: ${copied} files`);
if (missing.length > 0) console.log(`Missing: ${missing.join(', ')}`);

// ============================================================
// STEP 2: Upload to server and deploy
// ============================================================
console.log('\n=== Uploading to production server ===');

const cmd = `tar -czf - -C xm_film/sql seed-uploads | ssh -i "${SshKey}" ${Server} "cd /root/project_02 && tar -xzf - -C xm_film/sql"`;
execSync(cmd, { cwd: 'D:/project/project_02', stdio: 'inherit' });
console.log('Upload complete.');

console.log('\n=== Copying to Docker uploads volume ===');
execSync(`ssh -i "${SshKey}" ${Server} "docker cp /root/project_02/xm_film/sql/seed-uploads/. project-backend:/app/uploads/"`, { stdio: 'inherit' });
console.log('Files copied to uploads volume.');

console.log('\n=== Restarting backend ===');
execSync(`ssh -i "${SshKey}" ${Server} "docker restart project-backend"`, { stdio: 'inherit' });

console.log('\n=== All done! ===');
console.log('Backend restarting. Wait a few seconds and refresh the site.');
