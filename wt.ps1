param(
    [Parameter(Mandatory=$true, Position=0)]
    [ValidateSet("new", "done", "drop", "list")]
    [string]$Action,

    [Parameter(Position=1)]
    [string]$Name
)

$BaseDir = "D:\WorkSpace"
$MainRepo = "$BaseDir\signagepro"

switch ($Action) {

    "new" {
        if (-not $Name) { Write-Host "usage: .\wt.ps1 new [name]"; exit 1 }
        $Branch = "experiment/$Name"
        $Folder = "$BaseDir\signagepro-$Name"
        Set-Location $MainRepo
        git checkout dev
        git worktree add $Folder -b $Branch
        Write-Host ""
        Write-Host "[done] folder : $Folder" -ForegroundColor Green
        Write-Host "[done] branch : $Branch" -ForegroundColor Green
        Write-Host ""
        Write-Host "open: code $Folder" -ForegroundColor Cyan
    }

    "done" {
        if (-not $Name) { Write-Host "usage: .\wt.ps1 done [name]"; exit 1 }
        $Branch = "experiment/$Name"
        $Folder = "$BaseDir\signagepro-$Name"
        Set-Location $MainRepo
        git checkout dev
        git merge --squash $Branch
        Write-Host ""
        $Msg = Read-Host "commit message"
        git commit -m $Msg
        git push origin dev
        git worktree remove $Folder --force
        git branch -d $Branch
        Write-Host "[done] $Name -> dev merged" -ForegroundColor Green
    }

    "drop" {
        if (-not $Name) { Write-Host "usage: .\wt.ps1 drop [name]"; exit 1 }
        $Branch = "experiment/$Name"
        $Folder = "$BaseDir\signagepro-$Name"
        Set-Location $MainRepo
        git worktree remove $Folder --force
        git branch -D $Branch 2>$null
        Write-Host "[done] $Name dropped" -ForegroundColor Yellow
    }

    "list" {
        Set-Location $MainRepo
        Write-Host "worktree list:" -ForegroundColor Cyan
        git worktree list
    }
}
