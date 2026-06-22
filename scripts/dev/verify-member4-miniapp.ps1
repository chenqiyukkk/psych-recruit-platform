Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $repoRoot

Write-Host "[member4] Run mini-program utility tests"
node --test miniprogram\tests\miniprogram-utils.test.js

Write-Host "[member4] Validate mini-program page files"
@'
const fs = require('fs');
const path = require('path');
const root = process.cwd();
for (const file of ['miniprogram/app.json', 'miniprogram/project.config.json', 'miniprogram/sitemap.json']) {
  JSON.parse(fs.readFileSync(path.join(root, file), 'utf8'));
}
const app = JSON.parse(fs.readFileSync(path.join(root, 'miniprogram/app.json'), 'utf8'));
for (const page of app.pages) {
  for (const ext of ['js', 'wxml', 'wxss', 'json']) {
    const file = path.join(root, 'miniprogram', `${page}.${ext}`);
    if (!fs.existsSync(file)) throw new Error(`Missing ${file}`);
    if (ext === 'json') JSON.parse(fs.readFileSync(file, 'utf8'));
  }
}
require('./miniprogram/utils/request');
require('./miniprogram/utils/format');
console.log(`Validated ${app.pages.length} mini-program pages`);
'@ | node -

Write-Host "[member4] Check mini-program JavaScript syntax"
$files = git ls-files -- miniprogram | Where-Object { $_ -like "*.js" }
foreach ($file in $files) {
  node --check $file
}
Write-Host "Checked $($files.Count) JS files"

Write-Host "[member4] Done"
