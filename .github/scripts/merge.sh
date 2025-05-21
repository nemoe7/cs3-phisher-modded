# /.github/scripts/merge.sh

# Get directories deleted in fork
git diff --name-only --diff-filter=D 0a82eb78dab7b698939754d21c5adf3bcc841692 > delete
sed -n '/\/build.gradle.kts/p' delete | sed 's/\/.*/\//' | uniq | sort >> delete

# Prepare sync-upstream branch and merge
git checkout -B sync-upstream master
git merge upstream/master -X theirs --no-ff --quiet || true

# Re-delete files deleted in fork
if [ -s delete ]; then
  xargs git rm -f -r --ignore-unmatch < delete || true
fi

# Restore .github directory from origin/master
git checkout origin/master -- .github
