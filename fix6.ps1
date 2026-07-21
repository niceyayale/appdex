$utf8 = New-Object System.Text.UTF8Encoding($false)

# Fix AxmlEncoder.kt line 278
$f = "feature/feature-axml/src/main/java/com/appdex/axmleditor/AxmlEncoder.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match 'val regexNoNs = Regex') {
        $lines[$i] = '            val regexNoNs = Regex("""\s(\w+)="([^"]*)"""")'
        Write-Output "Fixed AxmlEncoder line $($i+1)"
    }
}
[System.IO.File]::WriteAllLines($f, $lines, $utf8)

# Fix ApkDiffScreen.kt lines 228-230 - remove duplicates and fix corrupted strings
$f = "feature/feature-diff/src/main/java/com/appdex/diff/ApkDiffScreen.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
for ($i = 0; $i -lt $lines.Count; $i++) {
    # Fix line 228: "錨犻T?" -> "Removed"
    if ($lines[$i] -match 'DiffStatCard\("錨犻T\?"') {
        $lines[$i] = '                DiffStatCard("Removed", result.removedCount, RedSupergiant, Modifier.weight(1f))'
        Write-Output "Fixed ApkDiffScreen line $($i+1)"
    }
    # Fix line 229: "淇"?, -> "Modified"
    if ($lines[$i] -match 'DiffStatCard\("淇"\?') {
        $lines[$i] = '                DiffStatCard("Modified", result.modifiedCount, AmberGold, Modifier.weight(1f))'
        Write-Output "Fixed ApkDiffScreen line $($i+1)"
    }
}
# Remove duplicate lines (line 230 is duplicate of 227)
$newLines = [System.Collections.Generic.List[string]]::new()
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($i -lt $lines.Count - 1 -and $lines[$i] -eq $lines[$i + 1]) {
        Write-Output "Removed duplicate at line $($i+1)"
        continue
    }
    $newLines.Add($lines[$i])
}
[System.IO.File]::WriteAllLines($f, $newLines.ToArray(), $utf8)

# Fix FtpClientManager.kt - add import android.util.Log
$f = "feature/feature-remote/src/main/java/com/appdex/remote/ftp/FtpClientManager.kt"
$content = [System.IO.File]::ReadAllText($f, $utf8)
if ($content -notmatch 'import android\.util\.Log') {
    $content = $content -replace '(^package .*$)', "`$1`nimport android.util.Log"
    [System.IO.File]::WriteAllText($f, $content, $utf8)
    Write-Output "Fixed FtpClientManager.kt"
}

# Fix WebServerScreen.kt - add import android.util.Log
$f = "feature/feature-remote/src/main/java/com/appdex/remote/server/WebServerScreen.kt"
$content = [System.IO.File]::ReadAllText($f, $utf8)
if ($content -notmatch 'import android\.util\.Log') {
    $content = $content -replace '(^package .*$)', "`$1`nimport android.util.Log"
    [System.IO.File]::WriteAllText($f, $content, $utf8)
    Write-Output "Fixed WebServerScreen.kt"
}

Write-Output "All done"
