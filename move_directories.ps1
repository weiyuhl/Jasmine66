# Move directories to match new package name
# Change com/google/samples/apps/nowinandroid to com/lhzkml/jasmine

Write-Host "Moving directories to match new package name..."

# Function to move directory structure
function Move-PackageDirectory {
    param(
        [string]$BasePath,
        [string]$OldPath,
        [string]$NewPath
    )
    
    $fullOldPath = Join-Path $BasePath $OldPath
    $fullNewPath = Join-Path $BasePath $NewPath
    
    if (Test-Path $fullOldPath) {
        # Create new directory structure
        $newDir = Split-Path $fullNewPath -Parent
        if (-not (Test-Path $newDir)) {
            New-Item -ItemType Directory -Path $newDir -Force | Out-Null
        }
        
        # Move the directory
        Move-Item -Path $fullOldPath -Destination $fullNewPath -Force
        Write-Host "Moved: $fullOldPath -> $fullNewPath"
        
        # Remove empty parent directories
        $parentDir = Split-Path $fullOldPath -Parent
        while ($parentDir -and $parentDir -ne $BasePath -and (Get-ChildItem $parentDir -Force | Measure-Object).Count -eq 0) {
            Remove-Item $parentDir -Force
            Write-Host "Removed empty directory: $parentDir"
            $parentDir = Split-Path $parentDir -Parent
        }
    }
}

# Process each module
$modules = @(
    "app\src\main\kotlin",
    "app\src\androidTest\kotlin",
    "app\src\test\kotlin",
    "app-nia-catalog\src\main\kotlin",
    "core\analytics\src\main\kotlin",
    "core\common\src\main\kotlin",
    "core\common\src\test\kotlin",
    "core\data\src\main\kotlin",
    "core\data\src\test\kotlin",
    "core\database\src\main\kotlin",
    "core\database\src\androidTest\kotlin",
    "core\datastore\src\main\kotlin",
    "core\datastore\src\test\kotlin",
    "core\datastore-proto\src\main\kotlin",
    "core\designsystem\src\main\kotlin",
    "core\designsystem\src\test\kotlin",
    "core\domain\src\main\kotlin",
    "core\domain\src\test\kotlin",
    "core\model\src\main\kotlin",
    "core\navigation\src\main\kotlin",
    "core\network\src\main\kotlin",
    "core\network\src\test\kotlin",
    "core\notifications\src\main\kotlin",
    "core\ui\src\main\kotlin",
    "core\ui\src\test\kotlin",
    "feature\bookmarks\api\src\main\kotlin",
    "feature\bookmarks\impl\src\main\kotlin",
    "feature\bookmarks\impl\src\test\kotlin",
    "feature\foryou\api\src\main\kotlin",
    "feature\foryou\impl\src\main\kotlin",
    "feature\foryou\impl\src\test\kotlin",
    "feature\interests\api\src\main\kotlin",
    "feature\interests\impl\src\main\kotlin",
    "feature\interests\impl\src\test\kotlin",
    "feature\search\api\src\main\kotlin",
    "feature\search\impl\src\main\kotlin",
    "feature\search\impl\src\test\kotlin",
    "feature\settings\impl\src\main\kotlin",
    "feature\settings\impl\src\test\kotlin",
    "feature\topic\api\src\main\kotlin",
    "feature\topic\impl\src\main\kotlin",
    "feature\topic\impl\src\test\kotlin"
)

foreach ($module in $modules) {
    $modulePath = Join-Path "d:\nowinandroid-main" $module
    if (Test-Path $modulePath) {
        $oldPackagePath = "com\google\samples\apps\nowinandroid"
        $newPackagePath = "com\lhzkml\jasmine"
        
        Move-PackageDirectory -BasePath $modulePath -OldPath $oldPackagePath -NewPath $newPackagePath
    }
}

# Also handle build-logic module
$buildLogicPath = "d:\nowinandroid-main\build-logic\convention\src\main\kotlin"
if (Test-Path $buildLogicPath) {
    $oldPackagePath = "com\google\samples\apps\nowinandroid"
    $newPackagePath = "com\lhzkml\jasmine"
    
    Move-PackageDirectory -BasePath $buildLogicPath -OldPath $oldPackagePath -NewPath $newPackagePath
}

Write-Host "Directory structure updated!"