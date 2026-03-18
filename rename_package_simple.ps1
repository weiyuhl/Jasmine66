# 包名更改脚本
# 将 com.google.samples.apps.nowinandroid 更改为 com.lhzkml.jasmine

$oldPackage = "com.google.samples.apps.nowinandroid"
$newPackage = "com.lhzkml.jasmine"

# 更改 Kotlin 文件中的包名
Write-Host "正在更改 Kotlin 文件中的包名..."
Get-ChildItem -Recurse -Filter "*.kt" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    if ($content -match [regex]::Escape($oldPackage)) {
        $newContent = $content -replace [regex]::Escape($oldPackage), $newPackage
        Set-Content -Path $_.FullName -Value $newContent -NoNewline
        Write-Host "已更新: $($_.FullName)"
    }
}

# 更改 XML 文件中的包名
Write-Host "正在更改 XML 文件中的包名..."
Get-ChildItem -Recurse -Filter "*.xml" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    if ($content -match [regex]::Escape($oldPackage)) {
        $newContent = $content -replace [regex]::Escape($oldPackage), $newPackage
        Set-Content -Path $_.FullName -Value $newContent -NoNewline
        Write-Host "已更新: $($_.FullName)"
    }
}

# 更改 build.gradle.kts 文件中的包名
Write-Host "正在更改 build.gradle.kts 文件中的包名..."
Get-ChildItem -Recurse -Filter "*.gradle.kts" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    if ($content -match [regex]::Escape($oldPackage)) {
        $newContent = $content -replace [regex]::Escape($oldPackage), $newPackage
        Set-Content -Path $_.FullName -Value $newContent -NoNewline
        Write-Host "已更新: $($_.FullName)"
    }
}

Write-Host "包名更改完成！"