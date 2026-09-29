# build_local.ps1 - AutoX.js 本地一键构建（v7 / v7_mini 签名 release）
# 用法：
#   .\build_local.ps1            # 默认构建 v7
#   .\build_local.ps1 v7_mini    # 构建体积更小的 v7_mini
# 若提示无法运行脚本，请用：powershell -ExecutionPolicy Bypass -File .\build_local.ps1
param(
    [ValidateSet("v7", "v7_mini")] [string]$Flavor = "v7"
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $root

Write-Host "AutoX.js 本地一键构建（flavor=$Flavor）" -ForegroundColor Cyan

# 0) 前置检查：Node.js（autojs:buildJsModule 需要）
if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Error "未找到 node，请先安装 Node.js 20+（详见 README 编译相关章节）"
    exit 1
}
$nodeVer = (node -v).TrimStart('v').Split('.')[0]
if ([int]$nodeVer -lt 20) { Write-Warning "检测到 Node v$nodeVer，README 建议使用 20+，可能出现兼容问题" }

# 1) 签名配置检查
$props = Join-Path $root "app/signing.properties"
$envReady = $false
if ($env:CI -eq "true" -and $env:KEYSTORE_BASE64) {
    $envReady = $true
} elseif (Test-Path $props) {
    $p = @{}
    Get-Content $props -Raw -ErrorAction SilentlyContinue | ForEach-Object {
        ($_ -split "`n") | ForEach-Object {
            if ($_ -match '^\s*([A-Z_]+)\s*=\s*(.+?)\s*$') { $p[$matches[1]] = $matches[2] }
        }
    }
    if ($p.STORE_FILE -and (Test-Path $p.STORE_FILE) -and $p.STORE_PASSWORD -and $p.KEY_ALIAS) {
        $envReady = $true
        Write-Host "✓ 使用本地签名配置：app/signing.properties" -ForegroundColor Green
    } else {
        Write-Warning "signing.properties 存在但配置不完整或 keystore 文件缺失"
    }
} else {
    Write-Warning "未发现签名配置（app/signing.properties 不存在，且非 CI 环境）。将产出【未签名】release，无法安装。若需签名请："
    Write-Warning "  1) 生成 keystore：keytool -genkeypair -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias autox"
    Write-Warning "  2) 创建 app/signing.properties，字段见 README「本地一键构建与签名」"
}

# 2) 执行构建链（自动串联 :autojs:buildJsModule -> app:buildTemplateApp -> assemble<Variant>Release）
$task = if ($Flavor -eq "v7_mini") { "buildV7MiniReleaseLocal" } else { "buildV7ReleaseLocal" }
Write-Host "▶ 开始构建 Gradle task：$task" -ForegroundColor Cyan
& ".\gradlew.bat" $task
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# 3) 汇总产物
$outDir = Join-Path $root "app/build/outputs/apk/$Flavor/release"
if (Test-Path $outDir) {
    Write-Host "✓ 构建完成，产物目录：$outDir" -ForegroundColor Green
    Get-ChildItem $outDir -Filter *.apk | ForEach-Object { Write-Host "  - $($_.FullName)" }
}
