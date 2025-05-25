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
    version=$(cat VERSION.build)
    image="{{image}}:${version}"
    echo "Building image: $image"
    docker buildx build --push --build-arg=VERSION=$version -t ${image} .

push-latest-image:
    #!/usr/bin/env bash
    version=$(cat VERSION)
    image="{{image}}:${version}"
    image_latest="{{image}}:latest"
    echo "Bumping latest tag: $image -> $image_latest "
    docker tag $image $image_latest
    docker push $image_latest

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

release: require-build-version commit-version push-latest-image create-release

commit-version:
    #!/usr/bin/env bash
    set -euxo pipefail

    version=$(cat {{version-file-build}})
    echo $version > {{version-file}}

    git add {{version-file}}
    git commit -m "[skip ci] Released version ${version}"
    git tag -a "v${version}" -m "Version ${version}"

    # Push commit and tag
    git push origin HEAD
    git push origin "v$version"

create-release:
    #!/usr/bin/env bash
    set -euxo pipefail

    version=$(cat {{version-file}})
    tag="v${version}"
    github_repo="${GITHUB_REPOSITORY:-}"
    github_token="${GITHUB_TOKEN:-}"
    release_name="Release ${tag}"
    release_body="Automated release."

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
    response=$(curl -s -X POST "https://api.github.com/repos/${github_repo}/releases" \
                    -H "Authorization: Bearer ${github_token}" \
                    -H "Accept: application/vnd.github+json" \
                    -d "${request}")

    release_id=$(echo "$response" | jq -r '.id')

    file_path="target/trino-k8s-${version}.zip"
    file_name=$(basename "$file_path")
    mime_type=$(file -b --mime-type "$file_path")

    upload_url="https://uploads.github.com/repos/${github_repo}/releases/${release_id}/assets?name=${file_name}"

    upload_response=$(curl -s -X POST "$upload_url" \
      -H "Authorization: Bearer ${github_token}" \
      -H "Content-Type: application/zip" \
      --data-binary @"${file_path}"
    )
