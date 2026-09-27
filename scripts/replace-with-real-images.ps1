# ============================================================
# Cinema System - Replace placeholder files with real images
# Matches real picture/video files to UUID filenames from
# data.sql, copies them to seed-uploads, then uploads to
# production server.
# ============================================================

param(
    [string]$PictureDir = "D:\project\project_02\picture",
    [string]$VideoDir = "D:\project\project_02\video",
    [string]$SeedDir = "D:\project\project_02\xm_film\sql\seed-uploads",
    [string]$SshKey = "D:\chrome_download\server_ssh_key_2026.pem",
    [string]$Server = "root@rjfwealth.cn",
    [string]$RemoteUploads = "/app/uploads"
)

# ============================================================
# MAPPING: real filename -> UUID (from data.sql)
# ============================================================

# Film posters: real filename -> UUID
$filmPosters = @{
    "749局.jpg"               = "2b8cd257-5ba1-43e3-9756-c920411cfb44.jpg"
    "伟大征程.jpg"             = "827002f2-3960-4712-a4f6-a7da746e49c6.jpg"
    "危机航线.jpg"             = "41a5b2c9-14b5-4d4d-9a27-16d3db09320c.jpg"
    "变形金刚：起源.jpg"        = "51fa4d27-e50f-44df-9dc3-293b930d93f6.jpg"
    "哈利·波特与凤凰社.jpg"     = "09cabb7f-c4fe-4908-ad11-d134ccdfd566.jpg"
    "哈利·波特与火焰杯.jpg"     = "bd88a9d6-b3f9-4fce-aa60-3ae4b745e613.jpg"
    "最后的里程.jpg"           = "9f01f773-6c3a-4c8d-85c1-ddcb47d74ac4.jpg"
    "暮然回首.jpg"             = "537c0725-77da-485f-a76a-c3891477640d.jpg"
    "毒液：最后一舞.jpg"       = "f6add364-ca35-4596-a1bc-9f6543228b72.jpg"
    "海绵宝宝.jpg"             = "dac64520-915f-4b61-95dc-3018c2390ebf.jpg"
    "熊猫计划.jpg"             = "b51ad4f5-25fa-4d8e-b13e-1f2ac6c4d3cc.jpg"
    "爱你很久很久.jpg"         = "af3145d2-cf73-473f-8690-4450da51690e.jpg"
    "爱情神话.jpg"             = "5d75701e-567b-4684-a618-a423130e4610.jpg"
    "红色一号：冬日行动.jpg"    = "e27d255c-186b-45b5-9dc5-e059f1faf269.jpg"
    "这个杀手不太冷.jpg"       = "fc8ed88c-2719-40ed-9f4b-b5efd34ade55.jpg"
    "那个不为人知的故事.jpg"    = "e993904e-2e34-4d32-80cc-c6695a00c6b9.jpg"
    "志愿军.jpg"               = "4253ae24-a226-45bf-b8c4-0fbd9824e3e2.jpg"
}

# Cinema logos
$cinemaLogos = @{
    "万象城影城.jpg"           = "4e62ebfa-30f3-4e85-9b3f-2526ecac48e3.jpg"
    "万达影城.jpg"             = "9e32dc92-5a1c-4ed5-bed2-4bef6f652380.jpg"
    "奥斯卡赛影城.jpg"         = "e2935134-646b-4a56-b81d-8b5fc07f728e.jpg"
    "丁丁影城.jpg"             = "06756954-4f38-4374-863a-c740b588e109.jpg"
}

# Actor photos (character img) - single images
$actorPhotos = @{
    "刘德华.jpg"               = "ea4343cc-6ba0-4a57-a2aa-bd4775ffae99.jpg"
    "张子枫.jpg"               = "15612c22-5124-4618-9c37-c489db4fc899.jpg"  # 危机航线
    "张子枫2.jpg"              = "6b8b8db0-0887-4541-a7fe-ccad09fd34c4.jpg"  # 志愿军
    "张梓宸.jpg"               = "2390faa7-14e7-4a80-a5e0-6e90a642a23b.jpg"
    "成龙.jpg"                 = "cea34574-05ee-4138-984b-d8af9f03bb0a.jpg"
    "朱一龙.jpg"               = "d6e7046b-35f0-4524-abb6-723f95ceb8da.jpg"
    "范欣.jpg"                 = "e5c0bbd6-2269-4bfe-9f9d-15340346a992.jpg"   # actor headshot
}

# Actor headshots (picture field)
$actorHeadshots = @{
    "刘德华.jpg"               = "9aefa1dd-3a59-4d22-a267-2b51e7aeff98.jpg"
    "成龙.jpg"                 = "2427debd-1a4d-4821-a658-70ca584358f8.jpg"
    "朱一龙.jpg"               = "8f324b5b-7fe1-4df9-8f22-8187040feec6.jpg"
}

# User/Admin avatars
$userAvatars = @{
    "admin.jpg"                = "fac908b2-616f-42e2-b880-a0df7b09ce56.jpg"
}

# Video covers
$videoCovers = @{
    "熊猫计划.jpg"             = "4343a492-ed82-4afb-bbad-3c3d864f15ac.jpg"
    "海绵宝宝.jpg"             = "f2b47831-9c0b-4863-b2f1-c350d29dcd1e.jpg"
    "毒液：最后一舞.jpg"       = "907179ba-a444-43e9-89aa-300dba2f8aba.jpg"
    "749局.jpg"                = "26de5bca-6c38-49dc-ae44-38a188319953.jpg"
    "暮然回首.jpg"             = "557d41b1-fec3-4f0d-98e1-b5fe6421680f.jpg"
    "变形金刚：起源.jpg"       = "2bf9d49e-26a8-45c1-804d-f79ee617dda2.jpg"
    "危机航线.jpg"             = "feb7370a-0912-44a2-bf85-d4a87f631dd8.jpg"
    "这个杀手不太冷.jpg"       = "fcff7c26-ab04-4212-ba0b-03b867aa997b.jpg"
    "志愿军.jpg"               = "e9a36369-61e5-4441-bc8d-54b6864d5272.jpg"
}

# Video MP4 files
$videos = @{
    "749.mp4"                     = "1090dbba-fdc5-4836-9f4a-323d250fdc13.mp4"
    "不为人知的故事.mp4"          = "f9d7ef46-ae54-43ba-9e4d-565f4613bff7.mp4"
    "危机航线宣传视频.mp4"        = "8a96e0ce-2f6c-45c9-90a1-9df1bc9b9d01.mp4"
    "变形金刚.mp4"                = "18ecdf7f-b9d7-4d99-994c-0353cf8774cf.mp4"
    "志愿军.mp4"                  = "12906d8e-e922-4251-99c4-6c9ae942fd72.mp4"
    "毒液.mp4"                    = "fa10ff2d-e7d5-4a9a-ae6e-d165809bfb50.mp4"
    "海绵宝宝.mp4"                = "3039214a-7d12-4dc6-b896-d80ecdbd714b.mp4"
    "熊猫计划.mp4"                = "f415156f-eee6-4bfe-9a24-a2f7734998cb.mp4"
    "蓦然回首.mp4"                = "9f0aeaf5-35b7-4da7-802e-741c8d75a075.mp4"
    "这个杀手不太冷.mp4"          = "e9207194-9e2f-49e1-94ee-a575d207b551.mp4"
}

# ============================================================
# ACTION: Copy real files to seed-uploads with UUID names
# ============================================================
Write-Host "=== Copying real files to seed-uploads ===" -ForegroundColor Cyan

$copied = 0; $skipped = 0; $missing = @()

# Combine all mappings into one
$allMappings = @{}
$filmPosters.GetEnumerator() | ForEach-Object { $allMappings[$_.Key] = $_.Value }
$cinemaLogos.GetEnumerator() | ForEach-Object { $allMappings[$_.Key] = $_.Value }
$actorPhotos.GetEnumerator() | ForEach-Object { $allMappings[$_.Key] = $_.Value }
# Note: actorHeadshots overlaps with actorPhotos (same source file, different UUID)
# We handle this separately
$userAvatars.GetEnumerator() | ForEach-Object { $allMappings[$_.Key] = $_.Value }
$videoCovers.GetEnumerator() | ForEach-Object { $allMappings[$_.Key] = $_.Value }

# Animal pics with no UUID match - skip these
Write-Host "`nUnmatched pictures (ignored): cat.jpg, 拉布拉多.jpg, 柴犬.jpg" -ForegroundColor Yellow

# Process picture files
foreach ($entry in $allMappings.GetEnumerator()) {
    $srcName = $entry.Key
    $dstName = $entry.Value
    $srcPath = Join-Path $PictureDir $srcName
    $dstPath = Join-Path $SeedDir $dstName

    if (Test-Path $srcPath) {
        Copy-Item -Path $srcPath -Destination $dstPath -Force
        Write-Host "  [OK] $srcName -> $dstName" -ForegroundColor Green
        $copied++
    } else {
        Write-Host "  [??] $srcName NOT FOUND" -ForegroundColor Red
        $missing += $srcName
    }
}

# Copy actor headshots too (same source files, different UUID dest)
foreach ($entry in $actorHeadshots.GetEnumerator()) {
    $srcName = $entry.Key
    $dstName = $entry.Value
    $srcPath = Join-Path $PictureDir $srcName
    $dstPath = Join-Path $SeedDir $dstName

    if (Test-Path $srcPath) {
        Copy-Item -Path $srcPath -Destination $dstPath -Force
        Write-Host "  [OK] $srcName -> $dstName (headshot)" -ForegroundColor Green
        $copied++
    }
}

# Process video files
foreach ($entry in $videos.GetEnumerator()) {
    $srcName = $entry.Key
    $dstName = $entry.Value
    $srcPath = Join-Path $VideoDir $srcName
    $dstPath = Join-Path $SeedDir $dstName

    if (Test-Path $srcPath) {
        Copy-Item -Path $srcPath -Destination $dstPath -Force
        Write-Host "  [OK] $srcName -> $dstName" -ForegroundColor Green
        $copied++
    } else {
        Write-Host "  [??] $srcName NOT FOUND" -ForegroundColor Red
        $missing += $srcName
    }
}

Write-Host "`n=== Done ===" -ForegroundColor Cyan
Write-Host "Copied: $copied files"
if ($missing.Count -gt 0) {
    Write-Host "Missing: $($missing -join ', ')" -ForegroundColor Yellow
}

# ============================================================
# ACTION: Upload to production server
# ============================================================
Write-Host "`n=== Uploading to production server ===" -ForegroundColor Cyan

# Tar the seed-uploads directory and pipe over SSH
$tarCmd = "tar -czf - -C xm_film/sql seed-uploads"
$sshCmd = "ssh -i `"$SshKey`" $Server `"cd /root/project_02 && tar -xzf - -C xm_film/sql`""

Write-Host "Running: tar | ssh to $Server..." -ForegroundColor Yellow

cd "D:\project\project_02"
$result = tar -czf - -C xm_film/sql seed-uploads | ssh -i "$SshKey" $Server "cd /root/project_02 && tar -xzf - -C xm_film/sql"

if ($LASTEXITCODE -eq 0) {
    Write-Host "Upload to /root/project_02/xm_film/sql/seed-uploads/ complete." -ForegroundColor Green
} else {
    Write-Host "Upload failed with exit code $LASTEXITCODE" -ForegroundColor Red
    exit 1
}

# Now copy from seed-uploads to the Docker uploads volume
Write-Host "`n=== Copying to Docker uploads volume ===" -ForegroundColor Cyan
$copyCmd = "docker cp /root/project_02/xm_film/sql/seed-uploads/. project-backend:/app/uploads/"

ssh -i "$SshKey" $Server $copyCmd
if ($LASTEXITCODE -eq 0) {
    Write-Host "Files copied to Docker uploads volume." -ForegroundColor Green
} else {
    Write-Host "docker cp failed, trying alternative..." -ForegroundColor Yellow
    # Alternative: copy inside container
    ssh -i "$SshKey" $Server "docker exec project-backend cp -r /app/seed-uploads/. /app/uploads/"
}

# Restart backend to ensure clean state
Write-Host "`n=== Restarting backend ===" -ForegroundColor Cyan
ssh -i "$SshKey" $Server "docker restart project-backend"

Write-Host "`n=== All done! ===" -ForegroundColor Cyan
Write-Host "Wait a few seconds, then refresh the site." -ForegroundColor Green
