#!/usr/bin/env bash

#!/bin/bash

VERSION_FILE="VERSION"

# Reads the version and returns components as array
read_version() {
  if [[ ! -f "$VERSION_FILE" ]]; then
    echo "VERSION file not found" >&2
    exit 1
  fi

  local version
  version=$(cat "$VERSION_FILE")
  IFS='.' read -r major minor rest <<< "$version"
  IFS='+' read -r patch _ <<< "$rest"  # strip off +metadata if present
  echo "$major $minor $patch"
}

# Write a new version string to the file
write_version() {
  local version="$1"
  echo "$version" > "$VERSION_FILE"
  echo "Updated VERSION to: $version"
}

# Determine if we're on main (supports both GitHub Actions and local use)
on_main_branch() {
  local branch="${GITHUB_REF##*/}"
  if [[ -z "$branch" ]]; then
    branch=$(git rev-parse --abbrev-ref HEAD)
  fi
  [[ "$branch" == "main" ]]
}

# Main function: bump or append timestamp
generate_version() {
  local part="$1"
  local version=($(read_version))
  local major=${version[0]}
  local minor=${version[1]}
  local patch=${version[2]}

  if on_main_branch; then
    case "$part" in
      major)
        ((major++)); minor=0; patch=0 ;;
      minor)
        ((minor++)); patch=0 ;;
      patch)
        ((patch++)) ;;
      *)
        echo "Invalid version part: $part"
        echo "Usage: $0 [major|minor|patch]"
        exit 1
        ;;
    esac
    new_version="${major}.${minor}.${patch}"
  else
    timestamp=$(date -u +%Y%m%dT%H%M%S)
    new_version="${major}.${minor}.${patch}-build.${timestamp}"
  fi

  write_version "$new_version"
}

# Entrypoint
main() {
  if [[ $# -ne 1 ]]; then
    echo "Usage: $0 [major|minor|patch]"
    exit 1
  fi

  generate_version "$1"
}

main "$@"
