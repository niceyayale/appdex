$utf8 = New-Object System.Text.UTF8Encoding($false)

$f = "feature/feature-security/src/main/java/com/appdex/security/SecurityScannerScreen.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)
# Fix line 162 (0-indexed = 161)
$lines[161] = '                ScanItem("Missing tamper protection")'
[System.IO.File]::WriteAllLines($f, $lines, $utf8)
Write-Output "Fixed SecurityScannerScreen.kt"