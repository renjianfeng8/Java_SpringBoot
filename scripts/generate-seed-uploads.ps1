# ============================================================
# Cinema Ticket System - Seed File Generator
# Generates placeholder images/videos for /files/ references
# in data.sql, fixing 404s on fresh Docker deployments.
# ============================================================

param(
    [string]$SeedDir = (Join-Path (Split-Path $PSScriptRoot -Parent) "xm_film/sql/seed-uploads"),
    [string]$DataSql = (Join-Path (Split-Path $PSScriptRoot -Parent) "xm_film/sql/data.sql")
)

Write-Host "=== Seed File Generator ===" -ForegroundColor Cyan
Write-Host "Output: $SeedDir"
Write-Host "Source: $DataSql"

# Ensure output directory exists
New-Item -ItemType Directory -Path $SeedDir -Force | Out-Null

# Extract all /files/* paths from data.sql
$matches = Select-String -Path $DataSql -Pattern '/files/[^'') ,]+' -AllMatches
$filePaths = $matches.Matches.Value | Sort-Object -Unique

Write-Host "Found $($filePaths.Count) file references" -ForegroundColor Yellow

$colors = @(
    "SkyBlue", "LightSalmon", "LightGreen", "LightCoral",
    "LightGoldenrodYellow", "LightPink", "LightSkyBlue",
    "LightSteelBlue", "Lavender", "PaleGreen", "Moccasin",
    "Thistle", "PowderBlue", "NavajoWhite", "Honeydew",
    "Bisque", "Azure", "MistyRose", "Wheat", "LemonChiffon"
)

Add-Type -AssemblyName System.Drawing | Out-Null

$created = 0; $skipped = 0
for ($i = 0; $i -lt $filePaths.Count; $i++) {
    $file = $filePaths[$i]
    $filename = [System.IO.Path]::GetFileName($file)
    $ext = [System.IO.Path]::GetExtension($file).ToLower()
    $outputPath = Join-Path $SeedDir $filename

    if (Test-Path $outputPath) { $skipped++; continue }

    if ($ext -eq ".jpg" -or $ext -eq ".png") {
        $colorName = $colors[$i % $colors.Count]
        $color = [System.Drawing.Color]::FromName($colorName)
        if ($color.IsKnownColor -eq $false) { $color = [System.Drawing.Color]::SkyBlue }

        $bmp = New-Object System.Drawing.Bitmap(300, 450)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.Clear($color)

        $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::DimGray, 2)
        $g.DrawRectangle($pen, 1, 1, 298, 448)

        $font = New-Object System.Drawing.Font("Arial", 9, [System.Drawing.FontStyle]::Bold)
        $brush = [System.Drawing.Brushes]::DimGray
        $fmt = New-Object System.Drawing.StringFormat
        $fmt.Alignment = "Center"
        $fmt.LineAlignment = "Center"
        $g.DrawString($filename, $font, $brush, 150, 180, $fmt)

        $font2 = New-Object System.Drawing.Font("Arial", 7)
        $g.DrawString("Placeholder - upload to replace", $font2, $brush, 150, 220, $fmt)

        $g.Dispose(); $font.Dispose(); $font2.Dispose(); $pen.Dispose(); $fmt.Dispose()

        if ($ext -eq ".jpg") {
            $bmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Jpeg)
        } else {
            $bmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
        }
        $bmp.Dispose()
        Write-Host "  [IMG] $filename" -ForegroundColor Green
        $created++
    }
    elseif ($ext -eq ".mp4") {
        # Minimal valid MP4: ftyp + moov boxes, no media data.
        # Enough for browsers to accept the file type and avoid 404s.
        $bytes = [byte[]]::new(32)
        # ftyp box (size=24)
        $bytes[0] = 0; $bytes[1] = 0; $bytes[2] = 0; $bytes[3] = 24
        # "ftyp"
        $bytes[4] = 0x66; $bytes[5] = 0x74; $bytes[6] = 0x79; $bytes[7] = 0x70
        # major brand "isom"
        $bytes[8] = 0x69; $bytes[9] = 0x73; $bytes[10] = 0x6F; $bytes[11] = 0x6D
        # minor version 0
        $bytes[12] = 0; $bytes[13] = 0; $bytes[14] = 0; $bytes[15] = 0
        # compatible "isom"
        $bytes[16] = 0x69; $bytes[17] = 0x73; $bytes[18] = 0x6F; $bytes[19] = 0x6D
        # compatible "mp42"
        $bytes[20] = 0x6D; $bytes[21] = 0x70; $bytes[22] = 0x34; $bytes[23] = 0x32
        # moov box (size=8)
        $bytes[24] = 0; $bytes[25] = 0; $bytes[26] = 0; $bytes[27] = 8
        # "moov"
        $bytes[28] = 0x6D; $bytes[29] = 0x6F; $bytes[30] = 0x6F; $bytes[31] = 0x76

        [System.IO.File]::WriteAllBytes($outputPath, $bytes)
        Write-Host "  [VID] $filename (32B placeholder)" -ForegroundColor Green
        $created++
    }
}

Write-Host ""
Write-Host "=== Done ===" -ForegroundColor Cyan
Write-Host "Created: $created files"
Write-Host "Skipped (existing): $skipped files"
Write-Host "Total referenced: $($filePaths.Count) files"
