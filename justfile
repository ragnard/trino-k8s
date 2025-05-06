build-type := "dev"
version-file := "VERSION"
version-file-build := version-file + ".build"
image := "ghcr.io/ragnard/trino-k8s"

version-type := if build-type == "dev" { "dev" } else { "patch" }

build: build-plugin build-image

prepare: (generate-build-version version-type)

build-plugin: prepare
    #!/usr/bin/env bash
    VERSION=$(cat VERSION.build)
    ./mvnw -Drevision=${VERSION} -B --no-transfer-progress -T2C package

build-image: prepare
    #!/usr/bin/env bash
    VERSION=$(cat VERSION.build)
    image="{{image}}:${VERSION}"
    echo "Building image: $image"
    docker build --build-arg=VERSION=$VERSION -t ${image} .

generate-build-version type=version-type:
    #!/usr/bin/env bash
    set -e
    version=$(cat "{{version-file}}")
    IFS='.' read -r major minor patch <<< "$version"

    case "{{type}}" in
      dev)
        timestamp=$(date -u +%Y%m%dT%H%M%S)
        branch=$(git rev-parse --abbrev-ref HEAD)
        version="${version}-build.${timestamp}"
        ;;
      patch)
        version="${major}.${minor}.$((patch+1))"
        ;;
      minor)
        version="${major}.$((minor+1)).0"
        ;;
      major)
        version="$((major+1)).0.0"
        ;;
      *)
        echo "Unknown version type: {{type}}"
        exit 1
        ;;
    esac
    echo "${version}" > {{version-file-build}}
    echo "Setting version to: ${version}"

require-build-version:
    #!/usr/bin/env bash
    if [ ! -f "{{ version-file-build}}" ]; then
      echo "Build version not found!"
      exit 1
    fi

#release: commit-version create-release
release: require-build-version commit-version create-release

commit-version:
    #!/usr/bin/env bash
    version=$(cat {{version-file-build}})
    echo "Releasing ${version}"
    echo $version > {{version-file}}

    # git config user.name "${GITHUB_ACTOR:-github-actions}"
    # git config user.email "${GITHUB_ACTOR:-github-actions}@users.noreply.github.com"

    git add {{version-file}}
    git commit -m "[skip ci] Released version ${version}"
    git tag -a "v${version}" -m "Version ${version}"

    # Push commit and tag
    git push origin HEAD
    git push origin "v$version"


create-release:
    #!/usr/bin/env bash
    version=$(cat {{version-file}})
    tag="v${version}"
    github_repo="${GITHUB_REPOSITORY:-}"
    github_token="${GITHUB_TOKEN:-}"
    release_name="Release ${tag}"
    release_body="Automated release."

    echo "📦 Creating GitHub release for tag $TAG..."

    request=$(cat <<EOF
    {
      "tag_name": "${tag}",
      "name": "${release_name}",
      "body": "${release_body}",
      "draft": false,
      "prerelease": false,
      "generate_release_notes": true
    }
    EOF
    )

    echo $request

    RESPONSE=$(curl -s -X POST "https://api.github.com/repos/${github_repo}/releases" \
                    -H "Authorization: Bearer ${github_token}" \
                    -H "Accept: application/vnd.github+json" \
                    -d "${request}")

    echo "✅ Release created:"
    echo "$RESPONSE" | grep "html_url" || echo "$RESPONSE"
