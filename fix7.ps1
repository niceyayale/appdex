$utf8 = New-Object System.Text.UTF8Encoding($false)

# Fix ApkDiffScreen.kt lines 227-230
$f = "feature/feature-diff/src/main/java/com/appdex/diff/ApkDiffScreen.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
$lines[226] = '                DiffStatCard("Added", result.addedCount, AuroraGreen, Modifier.weight(1f))'
$lines[227] = '                DiffStatCard("Removed", result.removedCount, RedSupergiant, Modifier.weight(1f))'
$lines[228] = '                DiffStatCard("Modified", result.modifiedCount, AmberGold, Modifier.weight(1f))'
$lines[229] = '                DiffStatCard("Same", result.sameCount, TextTertiary, Modifier.weight(1f))'
[System.IO.File]::WriteAllLines($f, $lines, $utf8)
Write-Output "Fixed ApkDiffScreen.kt"
