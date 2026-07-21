$utf8 = New-Object System.Text.UTF8Encoding($false)

# Fix SigningScreen.kt line 450 and 518
$f = "feature/feature-signing/src/main/java/com/appdex/signing/SigningScreen.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match 'SummaryRow\("杈撳嚭"\?\?') {
        $lines[$i] = '                SummaryRow("输出", state.inputApkName.replace(".apk", "_signed.apk"))'
    }
    if ($lines[$i] -match 'AppDexSection\(label = "杈撳嚭鏂囦欢"\?') {
        $lines[$i] = '        AppDexSection(label = "输出文件") {'
    }
}
[System.IO.File]::WriteAllLines($f, $lines, $utf8)
Write-Output "Fixed SigningScreen.kt"

# Fix HexEditorScreen.kt line 77
$f = "feature/feature-hex/src/main/java/com/appdex/hex/HexEditorScreen.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match 'val msg = if \(effect.resultCount') {
        $lines[$i] = '                    val msg = if (effect.resultCount > 0) "Found ${effect.resultCount} results" else "No results found"'
    }
}
[System.IO.File]::WriteAllLines($f, $lines, $utf8)
Write-Output "Fixed HexEditorScreen.kt"

# Fix ApkFile.kt lines 101,103 - extra quote in char literal
$f = "library/lib-apk/src/main/java/com/appdex/apk/ApkFile.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match "indexOf\('\"'\"") {
        $lines[$i] = $lines[$i] -replace "indexOf\('\"'\""", "indexOf('""'"
    }
}
[System.IO.File]::WriteAllLines($f, $lines, $utf8)
Write-Output "Fixed ApkFile.kt"

Write-Output "All done"
