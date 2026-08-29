#!/usr/bin/env bash
# CI driver for the TACZ Soldiers mod build (runs on GitHub Actions runners).
# All build logic lives here so that the workflow file never needs updating.
mkdir -p build-output libs
exec > >(tee -a build-output/ci.log) 2>&1
set -x

# ---------------------------------------------------------------------------
# 1) Optional: download TACZ (Forge, 1.20.1) from Modrinth for a runtime smoke test
# ---------------------------------------------------------------------------
TACZ=false
URL=""
for SLUG in tacz timeless-and-classics-zero tacz-guns; do
  echo "Trying Modrinth slug: ${SLUG}"
  URL=$(curl -sS --max-time 60 "https://api.modrinth.com/v2/project/${SLUG}/version" | python3 -c "
import json, sys
try:
    vs = json.load(sys.stdin)
except Exception:
    sys.exit(0)
for v in vs:
    loaders = v.get('loaders') or []
    game_versions = v.get('game_versions') or []
    if 'forge' in loaders and '1.20.1' in game_versions:
        files = sorted(v.get('files', []), key=lambda f: f.get('primary', False), reverse=True)
        if files:
            print(files[0]['url'])
            break
") || URL=""
  [ -n "${URL}" ] && break
done
echo "TACZ URL: ${URL}"

if [ -n "${URL}" ]; then
    if curl -sSL --max-time 300 -o libs/tacz.jar "${URL}"; then
        ls -la libs/tacz.jar
        TACZ=true
    else
        echo "TACZ download failed; skipping smoke test."
        rm -f libs/tacz.jar
    fi
else
    echo "Could not resolve TACZ from Modrinth; smoke test will be skipped."
fi
echo "tacz=${TACZ}" > build-output/tacz-status.txt

# ---------------------------------------------------------------------------
# 2) Build the mod
# ---------------------------------------------------------------------------
gradle build --no-daemon --stacktrace > build-output/build.log 2>&1
BUILD_RC=$?
echo "gradle build exit code: ${BUILD_RC}"
ls -la build/libs >> build-output/build.log 2>&1
tail -n 80 build-output/build.log

# ---------------------------------------------------------------------------
# 3) Optional runtime smoke test: run a Forge dev server with TACZ + our mod
# ---------------------------------------------------------------------------
if [ "${TACZ}" = "true" ] && [ "${BUILD_RC}" = "0" ]; then
    mkdir -p run
    echo "eula=true" > run/eula.txt
    ( timeout 420 gradle runServer --no-daemon > build-output/server.log 2>&1 || true )
    echo "--- server log markers ---"
    grep -E "Done|ERROR|FATAL|Exception|taczsoldiers" build-output/server.log | head -80 || true
fi

# ---------------------------------------------------------------------------
# 4) Publish results (jar + logs) back to the branch
# ---------------------------------------------------------------------------
git config user.name "arena-build-bot"
git config user.email "arena-build-bot@users.noreply.github.com"
cp -f build/libs/*.jar build-output/ 2>/dev/null || true
rm -f libs/tacz.jar
git add -f build-output
git commit -m "chore(ci): build artifacts and logs [skip ci]" || echo "nothing new to commit"
git pull --rebase origin "${GITHUB_REF_NAME:-arena/01a04c19-mod}" || true
git push origin "HEAD:${GITHUB_REF_NAME:-arena/01a04c19-mod}" || true

exit 0
