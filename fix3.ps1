$utf8 = New-Object System.Text.UTF8Encoding($false)

# Fix HexEditorScreen.kt line 77
$f = "feature/feature-hex/src/main/java/com/appdex/hex/HexEditorScreen.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match 'val msg = if \(effect.resultCount') {
        $lines[$i] = '                    val msg = if (effect.resultCount > 0) "Found ${effect.resultCount} results" else "No results found"'
        Write-Output "Fixed HexEditorScreen line $($i+1)"
    }
    if ($lines[$i] -match 'text = if \(isHexSearch\)') {
        $lines[$i] = '                                text = if (isHexSearch) "Hex search (e.g. 48656C6C6F)" else "ASCII search",'
        Write-Output "Fixed HexEditorScreen search line $($i+1)"
    }
}
[System.IO.File]::WriteAllLines($f, $lines, $utf8)

# Fix ApkFile.kt - lines with extra quote in char literal
$f = "library/lib-apk/src/main/java/com/appdex/apk/ApkFile.kt"
$content = [System.IO.File]::ReadAllText($f, $utf8)
# The pattern is: '"' followed by " which creates '"'"
# We need to replace '"' with just '"'
$dq = [char]34  # double quote
$bad = [string]$dq + [string]$dq
$good = [string]$dq
# Only replace in the context of indexOf
$content = $content -replace "indexOf\('""'""", "indexOf('""'"
[System.IO.File]::WriteAllText($f, $content, $utf8)
Write-Output "Fixed ApkFile.kt"

Write-Output "Done"
