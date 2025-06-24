:: Get directories deleted in fork
git diff --name-only --diff-filter=D 0a82eb78dab7b698939754d21c5adf3bcc841692 > delete.txt
findstr /R "\\build.gradle.kts" delete.txt > temp.txt
for /f "usebackq delims=" %%a in (temp.txt) do (
  echo %%~dpa
) | sort | uniq >> delete.txt

:: Prepare sync-upstream branch and merge
git checkout -B sync-upstream master
git merge upstream/master -X theirs --no-ff --quiet || rem continue even if merge fails

:: Re-delete files deleted in fork
for /f "usebackq delims=" %%a in (delete.txt) do (
  git rm -f -r --ignore-unmatch "%%a"
)

:: Restore .github directory from origin/master
git checkout origin/master -- .github

del delete.txt 2>nul
del temp.txt 2>nul
