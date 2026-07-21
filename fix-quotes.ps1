$utf8 = New-Object System.Text.UTF8Encoding($false)
$files = Get-ChildItem -Recurse -Filter "*.kt" | Where-Object { $_.FullName -notmatch '\.gradle|build\\' }

foreach ($f in $files) {
    $lines = [System.IO.File]::ReadAllLines($f.FullName, $utf8)
    $modified = $false

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        if ($line.TrimStart().StartsWith("//")) { continue }

        $qcount = ([regex]::Matches($line, '"')).Count

        if ($qcount -eq 1) {
            $qpos = $line.IndexOf('"')
            $rest = $line.Substring($qpos + 1)
            $insertPos = -1

            foreach ($ch in @(',', ')', '}')) {
                $pos = $rest.IndexOf($ch)
                if ($pos -ge 0 -and ($insertPos -lt 0 -or $pos -lt $insertPos)) {
                    $insertPos = $pos
                }
            }

            if ($insertPos -ge 0) {
                $absolutePos = $qpos + 1 + $insertPos
                $line = $line.Substring(0, $absolutePos) + '"' + $line.Substring($absolutePos)
                $lines[$i] = $line
                $modified = $true
            }
        }
    }

    if ($modified) {
        [System.IO.File]::WriteAllLines($f.FullName, $lines, $utf8)
        Write-Output "Fixed: $($f.Name)"
    }
}

Write-Output "Done"
