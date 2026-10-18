#
# Copyright (C) 2017-2026 Institute of Communication and Computer Systems (imu.iccs.gr)
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v2.0, unless 
# Esper library is used, in which case it is subject to the terms of General Public License v2.0.
# If a copy of the MPL was not distributed with this file, you can obtain one at 
# https://www.mozilla.org/en-US/MPL/2.0/
#


# ----------------- EMS client Builder image -----------------
FROM ems_core_builder_image AS ems-client-with-metricsfilter-builder

ARG BUILD_DIR=/build

# Accept optional metadata (CI can pass these, local builds ignore them)
ARG GIT_COMMIT=unknown
ARG GIT_BRANCH=unknown
ARG GIT_URL=unknown
ARG DOCKER_IMAGE=unknown
ARG BUILD_DESCR=''

ENV GIT_COMMIT=$GIT_COMMIT \
    GIT_BRANCH=$GIT_BRANCH \
    GIT_URL=$GIT_URL

WORKDIR ${BUILD_DIR}/ems-client-with-metricsfilter

# Download dependencies
COPY ./pom.xml      ${BUILD_DIR}/ems-client-with-metricsfilter/pom.xml
#RUN --mount=type=cache,target=/root/.m2,id=maven-cache mvn -B dependency:go-offline
RUN mvn -q -B -ntp dependency:go-offline

# Copy source
COPY .  ${BUILD_DIR}/ems-client-with-metricsfilter/

# Build plugin
#RUN --mount=type=cache,target=/root/.m2,id=maven-cache \
RUN \
    mvn -B -ntp -DskipTests \
    clean install \
    dependency:copy-dependencies


# -----------------   EMS client with metrics filter image   -----------------
FROM ems_core_image AS ems-client-with-metricsfilter

ARG BUILD_DIR=/build

ENV SCAN_PACKAGES=eu.nebulous.ems \
    IP_SETTING=DEFAULT_IP         \
    SELF_HEALING_ENABLED=false

RUN --mount=type=bind,from=ems-client-with-metricsfilter-builder,source=${BUILD_DIR}/ems-client-with-metricsfilter/target,target=/tmp/target \
    cp -f  /tmp/target/ems-client-with-metricsfilter-*.jar  /opt/baguette-client/jars/ && \
    cp -rn /tmp/target/dependency/*                         /opt/baguette-client/jars/
