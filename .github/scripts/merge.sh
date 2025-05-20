git remote add upstream https://github.com/phisher98/cloudstream-extensions-phisher.git || true
git fetch upstream

# Check if upstream is ahead
UPSTREAM_HASH=$(git rev-parse upstream/master)
ORIGIN_HASH=$(git rev-parse origin/master)
if [ "$UPSTREAM_HASH" = "$ORIGIN_HASH" ]; then
  echo "Upstream is not ahead. Exiting."
  exit 78
fi

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
