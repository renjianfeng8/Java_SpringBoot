#!/bin/sh
# ============================================================
# Cinema Ticket System - Docker Entrypoint
# Seeds the uploads directory with placeholder files on first
# start, then launches the Spring Boot app.
# ============================================================
set -e

SEED_DIR="${SEED_DIR:-/app/seed-uploads}"
UPLOAD_DIR="${FILE_UPLOAD_DIR:-/app/uploads}"

# Seed uploads directory if empty (first start with fresh volume)
if [ -d "$SEED_DIR" ] && [ "$(ls -A "$SEED_DIR" 2>/dev/null)" ]; then
    if [ ! -d "$UPLOAD_DIR" ] || [ -z "$(ls -A "$UPLOAD_DIR" 2>/dev/null)" ]; then
        echo "=== Seeding uploads directory from $SEED_DIR ==="
        mkdir -p "$UPLOAD_DIR"
        cp -r "$SEED_DIR"/* "$UPLOAD_DIR/"
        echo "=== Seed complete: $(ls -1 "$UPLOAD_DIR" | wc -l) files ==="
    else
        echo "Uploads directory already populated, skipping seed."
    fi
else
    echo "No seed directory found at $SEED_DIR, skipping seed."
fi

# Launch the Spring Boot application
exec java -jar app.jar "$@"
