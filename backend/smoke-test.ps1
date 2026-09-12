$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:8082'

# 1) login
$login = Invoke-RestMethod -Uri "$base/api/login" -Method Post -ContentType 'application/json' `
  -Body '{"username":"star","password":"star2021"}'
Write-Output "1 LOGIN ok user=$($login.username) tokenLen=$($login.token.Length)"
$h = @{ Authorization = "Bearer $($login.token)" }

# 2) anon list (no token)
$anon = Invoke-RestMethod -Uri "$base/api/photos"
Write-Output "2 LIST(anon) count=$($anon.Count)"

# 3) upload 2 test jpegs (multipart)
Add-Type -AssemblyName System.Net.Http
$client = [System.Net.Http.HttpClient]::new()
$client.DefaultRequestHeaders.Add('Authorization', "Bearer $($login.token)")

function New-JpegFile($path) {
  $b64 = '/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/wAALCAABAAEBAREA/8QAFAABAAAAAAAAAAAAAAAAAAAACf/EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAD8AVN//2Q=='
  [IO.File]::WriteAllBytes($path, [Convert]::FromBase64String($b64))
}
New-JpegFile "$env:TEMP\t1.jpg"
New-JpegFile "$env:TEMP\t2.jpg"

$content = [System.Net.Http.MultipartFormDataContent]::new()
foreach ($f in @("$env:TEMP\t1.jpg", "$env:TEMP\t2.jpg")) {
  $bytes = [IO.File]::ReadAllBytes($f)
  $sc = [System.Net.Http.ByteArrayContent]::new($bytes)
  $sc.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse('image/jpeg')
  $content.Add($sc, 'files', [IO.Path]::GetFileName($f))
}
$metas = '[{"takenAt":"2024-05-01","note":"first test note","featured":true},{"takenAt":"2023-08-15","note":"second test","featured":false}]'
$mc = [System.Net.Http.StringContent]::new($metas, [Text.Encoding]::UTF8, 'application/json')
$content.Add($mc, 'metas')

$resp = $client.PostAsync("$base/api/photos", $content).Result
$body = $resp.Content.ReadAsStringAsync().Result
Write-Output "3 UPLOAD status=$([int]$resp.StatusCode) body=$body"
$created = $body | ConvertFrom-Json
$id1 = $created[0].id

# 4) update
$upd = Invoke-RestMethod -Uri "$base/api/photos/$id1" -Method Patch -Headers $h -ContentType 'application/json' `
  -Body '{"note":"edited note","featured":false,"takenAt":"2024-05-02"}'
Write-Output "4 UPDATE note=$($upd.note) featured=$($upd.featured) takenAt=$($upd.takenAt)"

# 5) trash / restore
Invoke-RestMethod -Uri "$base/api/photos/$id1/trash" -Method Post -Headers $h | Out-Null
$tr = Invoke-RestMethod -Uri "$base/api/photos?trashed=true" -Headers $h
Write-Output "5 TRASH trashedCount=$($tr.Count)"
Invoke-RestMethod -Uri "$base/api/photos/$id1/restore" -Method Post -Headers $h | Out-Null
$li = Invoke-RestMethod -Uri "$base/api/photos" -Headers $h
Write-Output "5 RESTORE listCount=$($li.Count)"

# 6) destroy
Invoke-RestMethod -Uri "$base/api/photos/$id1/final" -Method Delete -Headers $h | Out-Null
$li2 = Invoke-RestMethod -Uri "$base/api/photos" -Headers $h
Write-Output "6 DESTROY listCount=$($li2.Count)"

# 7) static file accessible
$u = $created[1].url
$static = Invoke-WebRequest -Uri "$base$u" -Method Head
Write-Output "7 STATIC $u status=$([int]$static.StatusCode)"

# 8) write without token should be 401
try {
  Invoke-RestMethod -Uri "$base/api/photos/$($created[1].id)/trash" -Method Post | Out-Null
  Write-Output "8 AUTH FAIL: no token was accepted"
} catch {
  Write-Output "8 AUTH blocked status=$($_.Exception.Response.StatusCode.value__)"
}

Remove-Item "$env:TEMP\t1.jpg", "$env:TEMP\t2.jpg" -ErrorAction SilentlyContinue
$client.Dispose()
Write-Output "ALL DONE"
