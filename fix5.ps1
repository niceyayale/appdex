$utf8 = New-Object System.Text.UTF8Encoding($false)

# Fix SyntaxHighlighter.kt - lines with \[ or \(?: patterns
$f = "library/lib-syntax/src/main/java/com/appdex/syntax/SyntaxHighlighter.kt"
$content = [System.IO.File]::ReadAllText($f, $utf8)
# Fix: Regex("\[ -> Regex("\"[
$content = $content.Replace('Regex("\[', 'Regex("\"[')
# Fix: \(?: -> \"(?:
$content = $content.Replace('\(?:', '\"(?:')
[System.IO.File]::WriteAllText($f, $content, $utf8)
Write-Output "Fixed SyntaxHighlighter.kt"

# Fix AxmlEncoder.kt line 273
$f = "feature/feature-axml/src/main/java/com/appdex/axmleditor/AxmlEncoder.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match 'val regex = Regex\(') {
        $lines[$i] = '            val regex = Regex("""(\S+?):(\S+?)="([^"]*)"""")'
        Write-Output "Fixed AxmlEncoder line $($i+1)"
    }
}
[System.IO.File]::WriteAllLines($f, $lines, $utf8)
Write-Output "Fixed AxmlEncoder.kt"

# Fix DexBrowserScreen.kt
$f = "feature/feature-dex/src/main/java/com/appdex/dex/DexBrowserScreen.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match 'text = if \(state.searchQuery') {
        $lines[$i] = '                    text = if (state.searchQuery.isNotEmpty()) "No matching classes found" else "No classes",'
        Write-Output "Fixed DexBrowser line $($i+1)"
    }
    if ($lines[$i] -match 'text = "\$\{state.selectedDexName"') {
        $lines[$i] = '                text = "${state.selectedDexName} · ${state.smaliCode.count { it == ''\n'' }} lines",'
        Write-Output "Fixed DexBrowser line $($i+1)"
    }
}
[System.IO.File]::WriteAllLines($f, $lines, $utf8)
Write-Output "Fixed DexBrowserScreen.kt"

# Fix ApkDiffScreen.kt
$f = "feature/feature-diff/src/main/java/com/appdex/diff/ApkDiffScreen.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match 'DiffStatCard\("淇"\?') {
        $lines[$i] = '                DiffStatCard("Modified", result.modifiedCount, AmberGold, Modifier.weight(1f))'
        Write-Output "Fixed ApkDiffScreen line $($i+1)"
    }
    if ($lines[$i] -match 'Text\(text = "  \.\.\.') {
        $lines[$i] = '            Text(text = "  ...${items.size - 5} more", fontSize = 9.sp, color = TextTertiary)'
        Write-Output "Fixed ApkDiffScreen line $($i+1)"
    }
}
[System.IO.File]::WriteAllLines($f, $lines, $utf8)
Write-Output "Fixed ApkDiffScreen.kt"

Write-Output "All done"
