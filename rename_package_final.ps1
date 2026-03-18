# Package rename script
# Change com.google.samples.apps.nowinandroid to com.lhzkml.jasmine

$oldPackage = "com.google.samples.apps.nowinandroid"
$newPackage = "com.lhzkml.jasmine"

# Change package names in Kotlin files
Write-Host "Changing package names in Kotlin files..."
Get-ChildItem -Recurse -Filter "*.kt" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    if ($content -match [regex]::Escape($oldPackage)) {
        $newContent = $content -replace [regex]::Escape($oldPackage), $newPackage
        Set-Content -Path $_.FullName -Value $newContent -NoNewline
        Write-Host "Updated: $($_.FullName)"
    }
}

# Change package names in XML files
Write-Host "Changing package names in XML files..."
Get-ChildItem -Recurse -Filter "*.xml" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    if ($content -match [regex]::Escape($oldPackage)) {
        $newContent = $content -replace [regex]::Escape($oldPackage), $newPackage
        Set-Content -Path $_.FullName -Value $newContent -NoNewline
        Write-Host "Updated: $($_.FullName)"
    }
}

# Change package names in build.gradle.kts files
Write-Host "Changing package names in build.gradle.kts files..."
Get-ChildItem -Recurse -Filter "*.gradle.kts" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    if ($content -match [regex]::Escape($oldPackage)) {
        $newContent = $content -replace [regex]::Escape($oldPackage), $newPackage
        Set-Content -Path $_.FullName -Value $newContent -NoNewline
        Write-Host "Updated: $($_.FullName)"
    }
}

Write-Host "Package rename completed!"