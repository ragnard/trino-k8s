ARG VERSION
ARG TRINO_VERSION=474

FROM alpine AS builder
ARG VERSION
ARG TRINO_VERSION

WORKDIR /plugin/trino-k8s

COPY target/trino-k8s-$VERSION.zip .
RUN unzip trino-k8s-$TRINO_K8S_VERSION.zip && rm trino-k8s-$TRINO_K8S_VERSION.zip
RUN mv trino-k8s-$TRINO_K8S_VERSION trino-k8s
RUN chmod 755 trino-k8s

FROM trinodb/trino-core:$TRINO_VERSION
ARG TRINO_VERSION

COPY --from=builder --chown=trino:trino /plugin/trino-k8s /usr/lib/trino/plugin/
