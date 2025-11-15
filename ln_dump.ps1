$path = 'src/main/java/com/candyrealms/candycore/modules/heads/HeadsModule.java'
$lines = Get-Content $path
for ($i=0; $i -lt $lines.Length; $i++) {
  $ln = $i + 1
  $line = $lines[$i]
  if ($line -match 'class HeadsModule|ensureTokenManager\(|addTokens\(|getPlayerHead\(|reloadConfig\(') {
    Write-Output ("{0}:{1}: {2}" -f $path, $ln, $line)
  }
}
