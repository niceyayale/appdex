$utf8 = New-Object System.Text.UTF8Encoding($false)

# Fix SyntaxHighlighter.kt - all lines with Regex("\(?: pattern
$f = "library/lib-syntax/src/main/java/com/appdex/syntax/SyntaxHighlighter.kt"
$content = [System.IO.File]::ReadAllText($f, $utf8)
# Replace Regex("\(?: with Regex("\"(?:
$content = $content.Replace('Regex("\(?:', 'Regex("\"(?:')
[System.IO.File]::WriteAllText($f, $content, $utf8)
Write-Output "Fixed SyntaxHighlighter.kt"

# Fix BinaryXmlDecoder.kt
$f = "library/lib-apk/src/main/java/com/appdex/apk/BinaryXmlDecoder.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
for ($i = 0; $i -lt $lines.Count; $i++) {
    # Fix: sb.append("=\") -> sb.append("=\"")
    if ($lines[$i] -eq '            sb.append("=\")') {
        $lines[$i] = '            sb.append("=\"")'
        Write-Output "Fixed BinaryXmlDecoder line $($i+1)"
    }
    # Fix: sb.append("\") -> sb.append("\"")
    if ($lines[$i] -eq '            sb.append("\")') {
        $lines[$i] = '            sb.append("\"")'
        Write-Output "Fixed BinaryXmlDecoder line $($i+1)"
    }
    # Fix: .replace("\", "&quot;") -> .replace("\"", "&quot;")
    if ($lines[$i] -eq '            .replace("\", "&quot;")') {
        $lines[$i] = '            .replace("\"", "&quot;")'
        Write-Output "Fixed BinaryXmlDecoder line $($i+1)"
    }
}
[System.IO.File]::WriteAllLines($f, $lines, $utf8)
Write-Output "Fixed BinaryXmlDecoder.kt"

Write-Output "All done"
