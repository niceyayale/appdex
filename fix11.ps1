$utf8 = New-Object System.Text.UTF8Encoding($false)

$f = "feature/feature-repack/src/main/java/com/appdex/repack/RepackagingScreen.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
$lines[376] = '                    text = if (result?.success == true) "Repack successful" else "Repack failed",'
[System.IO.File]::WriteAllLines($f, $lines, $utf8)
Write-Output "Fixed RepackagingScreen.kt line 377"