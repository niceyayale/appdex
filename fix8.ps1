$utf8 = New-Object System.Text.UTF8Encoding($false)

$f = "feature/feature-signing/src/main/java/com/appdex/signing/SigningScreen.kt"
$lines = [System.IO.File]::ReadAllLines($f, $utf8)

# Line 183 closes the Box prematurely - remove it
# Lines 185-217 should be indented to 8 spaces (inside Box)
# Line 218 closes the Box
# Line 219 closes the function

# Remove line 183 (premature Box closing)
$newLines = [System.Collections.Generic.List[string]]::new()
for ($i = 0; $i -lt $lines.Count; $i++) {
    $line = $lines[$i]
    if ($i -eq 182) {
        # Skip this line (the premature Box closing)
        continue
    }
    
    # Re-indent lines 184-217 (originally at 4 spaces) to 8 spaces
    if ($i -ge 183 -and $i -le 216) {
        $newLines.Add("        " + $line.Trim())
    } else {
        $newLines.Add($line)
    }
}

[System.IO.File]::WriteAllLines($f, $newLines.ToArray(), $utf8)
Write-Output "Fixed SigningScreen.kt Box structure"