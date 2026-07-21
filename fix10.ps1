$utf8 = New-Object System.Text.UTF8Encoding($false)

$f = "feature/feature-repack/src/main/java/com/appdex/repack/RepackagingScreen.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
for ($i = 0; $i -lt $lines.Count; $i++) {
    # Fix line 377 (0-indexed = 376)
    if ($lines[$i] -match 'text = if \(result\?\.success == true\) "?z') {
        $lines[$i] = '                    text = if (result?.success == true) "Repack successful" else "Repack failed",'
    }
}
[System.IO.File]::WriteAllLines($f, $lines, $utf8)
Write-Output "Fixed RepackagingScreen.kt"