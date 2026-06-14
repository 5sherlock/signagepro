git stash
git fetch --all
git checkout main
git reset --hard origin/main
git stash pop
pm2 restart all
pm2 status
git log --oneline -5
