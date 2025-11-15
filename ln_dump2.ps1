$p='src/main/resources/heads.yml'
$lines = Get-Content $p
for ($i=0; $i -lt $lines.Length; $i++) {
  $ln=$i+1
  $line=$lines[$i]
  if ($line -match 'vulcan-tokens-key|min-token-balance|debug-vulcan-tokens|messages:|head-item:') {
    Write-Output ("{0}:{1}: {2}" -f $p, $ln, $line)
  }
}
