build-type := "dev"
version-file := "VERSION"
version-file-build := version-file + ".build"
image := "ghcr.io/ragnard/trino-k8s"

version-type := if build-type == "dev" { "dev" } else { "patch" }

build: build-plugin build-image

build-plugin: generate-build-version
    #!/usr/bin/env bash
    VERSION=$(cat VERSION.build)
    ./mvnw -Drevision=${VERSION} -B --no-transfer-progress -T2C package

build-image: generate-build-version build-plugin
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
