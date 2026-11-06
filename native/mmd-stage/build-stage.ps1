param(
    [string]$BulletSource = "",
    [switch]$Install
)

$ErrorActionPreference = "Stop"
$stageProjectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..')).Path
$bulletRevision = '63c4d67e337017f9d8b298c900e9aabdb69296e7'
if (-not $BulletSource) {
    $bulletCheckout = Join-Path $stageProjectRoot '.native-deps\bullet3'
    if (-not (Test-Path -LiteralPath $bulletCheckout)) {
        New-Item -ItemType Directory -Path $bulletCheckout -Force | Out-Null
        git -C $bulletCheckout init
        if ($LASTEXITCODE -ne 0) { throw 'Could not initialize Bullet checkout' }
        git -C $bulletCheckout fetch --depth 1 https://github.com/bulletphysics/bullet3.git $bulletRevision
        if ($LASTEXITCODE -ne 0) { throw 'Could not fetch pinned Bullet source' }
        git -C $bulletCheckout checkout --detach FETCH_HEAD
        if ($LASTEXITCODE -ne 0) { throw 'Could not check out pinned Bullet source' }
    }
    $actualRevision = git -C $bulletCheckout rev-parse HEAD
    if ($LASTEXITCODE -ne 0 -or $actualRevision -ne $bulletRevision) {
        throw 'Existing Bullet checkout does not match the pinned revision'
    }
    $BulletSource = Join-Path $bulletCheckout 'src'
}
if (-not (Test-Path -LiteralPath (Join-Path $BulletSource 'btBulletDynamicsCommon.h'))) {
    throw 'BulletSource must point to Bullet src/'
}
$stagePreviousBulletSource = $env:MMD_BULLET_SOURCE
try {
    $env:MMD_BULLET_SOURCE = (Resolve-Path -LiteralPath $BulletSource).Path
    cargo test --manifest-path (Join-Path $PSScriptRoot 'Cargo.toml') --locked --lib
    if ($LASTEXITCODE -ne 0) { throw 'Stage runtime tests failed' }
    cargo build --manifest-path (Join-Path $PSScriptRoot 'Cargo.toml') --locked --release --lib
    if ($LASTEXITCODE -ne 0) { throw 'Stage runtime build failed' }
    $stageArtifact = Join-Path $PSScriptRoot 'target\release\mmd_engine.dll'
    Get-FileHash -LiteralPath $stageArtifact -Algorithm SHA256
    if ($Install) {
        $stageResource = Join-Path $stageProjectRoot 'src\main\resources\natives\windows-x64\mmd_engine.dll'
        $stageBackupDirectory = Join-Path $stageProjectRoot '.codex-backups'
        New-Item -ItemType Directory -Path $stageBackupDirectory -Force | Out-Null
        if (Test-Path -LiteralPath $stageResource) {
            $stageOldHash = (Get-FileHash -LiteralPath $stageResource -Algorithm SHA256).Hash.ToLowerInvariant()
            $stageBackup = Join-Path $stageBackupDirectory "mmd-engine-$stageOldHash.dll"
            if (-not (Test-Path -LiteralPath $stageBackup)) {
                Copy-Item -LiteralPath $stageResource -Destination $stageBackup
            }
        }
        Copy-Item -LiteralPath $stageArtifact -Destination $stageResource -Force
        Write-Output 'Stage DLL copied to source resources. Restart Minecraft after rebuilding the mod.'
    }
} finally {
    $env:MMD_BULLET_SOURCE = $stagePreviousBulletSource
}
