$utf8 = New-Object System.Text.UTF8Encoding($false)

# Fix HexEditorScreen.kt
$f = "feature/feature-hex/src/main/java/com/appdex/hex/HexEditorScreen.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
$lines[76] = '                    val msg = if (effect.resultCount > 0) "找到 ${effect.resultCount} 个结果" else "未找到结果"'
$lines[204] = '                                text = if (isHexSearch) "十六进制搜索 (如 48656C6C6F)" else "ASCII 搜索",'
[System.IO.File]::WriteAllLines($f, $lines, $utf8)
Write-Output "Fixed HexEditorScreen.kt"

# Fix WebFileServer.kt line 324
$f = "feature/feature-remote/src/main/java/com/appdex/remote/server/WebFileServer.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
$line324 = @'
            .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
'@
$lines[323] = $line324.TrimEnd("`r","`n")
[System.IO.File]::WriteAllLines($f, $lines, $utf8)
Write-Output "Fixed WebFileServer.kt"

# Fix SyntaxHighlighter.kt line 40
$f = "library/lib-syntax/src/main/java/com/appdex/syntax/SyntaxHighlighter.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
$line40 = @'
        HighlightRule(Regex("\"(?:[^\"\\\\]|\\\\.)*\""), SpanStyle(color = stringColor)),
'@
$lines[39] = $line40.TrimEnd("`r","`n")
[System.IO.File]::WriteAllLines($f, $lines, $utf8)
Write-Output "Fixed SyntaxHighlighter.kt"

# Fix SigningScreen.kt
$f = "feature/feature-signing/src/main/java/com/appdex/signing/SigningScreen.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
for ($i = 0; $i -lt $lines.Count; $i++) {
    $line = $lines[$i]
    if ($line -match 'trim\(\)\.ifEmpty' -and $line -match '"\?\?') {
        $lines[$i] = '                }.trim().ifEmpty { "N/A" })'
    }
    if ($line -match 'SummaryRow\("[^A-Za-z]') {
        $lines[$i] = $line -replace 'SummaryRow\("[^"]*"', 'SummaryRow("输出"'
    }
    if ($line -match 'AppDexSection\(label = "[^A-Za-z]') {
        $lines[$i] = $line -replace 'AppDexSection\(label = "[^"]*"', 'AppDexSection(label = "输出文件"'
    }
    if ($line -match 'text = if \(result\?\.success == true\) "' -and $line -match 'else') {
        $lines[$i] = '                    text = if (result?.success == true) "签名成功" else "签名完成",'
    }
}
[System.IO.File]::WriteAllLines($f, $lines, $utf8)
Write-Output "Fixed SigningScreen.kt"

Write-Output "All done"
