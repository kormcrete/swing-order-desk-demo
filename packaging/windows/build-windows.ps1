# Builds kormcrete-order-desk-windows-x64.zip: a "Kormcrete Order Desk" folder with the .exe (mvn -B -Pwindows
# package), the bundled Zulu JRE 8 (x64), the jars, Order Desk's plugin (order-desk-plugin/target), its recorded tests
# (app\uitest), LICENSE and NOTICE.
# Usage: packaging/windows/build-windows.ps1 <jre-home> <out-dir>
param(
    [Parameter(Mandatory = $true)] [string] $JreHome,
    [Parameter(Mandatory = $true)] [string] $OutDir
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$name = 'Kormcrete Order Desk'

if (-not (Test-Path (Join-Path $JreHome 'bin\javaw.exe')) -or -not (Test-Path (Join-Path $JreHome 'lib\rt.jar'))) {
    throw "$JreHome is not a Java 8 JRE (needs bin\javaw.exe and lib\rt.jar)"
}
$exe = Join-Path $root "target\windows\$name.exe"
$jars = @((Join-Path $root 'target\order-desk.jar'), (Join-Path $root 'target\swing-ui-agent-agent.jar'),
    (Join-Path $root 'order-desk-plugin\target\order-desk-plugin.jar'))
foreach ($f in @($exe) + $jars) {
    if (-not (Test-Path $f)) { throw "missing $f (run mvn -B -f order-desk-plugin/pom.xml install and mvn -B -Pwindows package first)" }
}

$work = Join-Path $root 'target\windows-x64'
if (Test-Path $work) { Remove-Item -Recurse -Force $work }
$dir = Join-Path $work $name
New-Item -ItemType Directory -Force -Path (Join-Path $dir 'app') | Out-Null
Copy-Item $exe $dir
Copy-Item -Recurse $JreHome (Join-Path $dir 'jre')
Copy-Item $jars (Join-Path $dir 'app')
New-Item -ItemType Directory -Force -Path (Join-Path $dir 'app\uitest') | Out-Null
Copy-Item (Join-Path $root 'uitest\*.json') (Join-Path $dir 'app\uitest')
Copy-Item (Join-Path $root 'LICENSE'), (Join-Path $root 'NOTICE') $dir

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$zip = Join-Path (Resolve-Path $OutDir).Path 'kormcrete-order-desk-windows-x64.zip'
if (Test-Path $zip) { Remove-Item $zip }
Compress-Archive -Path $dir -DestinationPath $zip
Write-Output $zip
