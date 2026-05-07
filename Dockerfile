#
# Copyright (C) 2017-2026 Institute of Communication and Computer Systems (imu.iccs.gr)
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v2.0, unless 
# Esper library is used, in which case it is subject to the terms of General Public License v2.0.
# If a copy of the MPL was not distributed with this file, you can obtain one at 
# https://www.mozilla.org/en-US/MPL/2.0/
#

ARG EMS_CORE_IMAGE=ems-server:8.0.0-SNAPSHOT
ARG EMS_CORE_BUILDER_IMAGE=ems-server-core-builder:latest

# ----------------- EMS Builder image -----------------
FROM $EMS_CORE_BUILDER_IMAGE AS ems-nebulous-translator-builder

ARG BUILD_DIR=/build

# Accept optional metadata (CI can pass these, local builds ignore them)
ARG GIT_COMMIT=unknown
ARG GIT_BRANCH=unknown
ARG GIT_URL=unknown
ARG DOCKER_IMAGE=unknown
ARG BUILD_DESCR=''

ENV GIT_COMMIT=$GIT_COMMIT \
    GIT_BRANCH=$GIT_BRANCH \
    GIT_URL=$GIT_URL \
    DOCKER_IMAGE=$DOCKER_IMAGE \
    BUILD_DESCR="$BUILD_DESCR"

WORKDIR ${BUILD_DIR}

COPY ./.git                    ${BUILD_DIR}/.git
COPY pom.xml                   ${BUILD_DIR}/pom.xml
RUN sed -i 's|<module>\.\./ems-main/ems-core</module>|<module>ems-core</module>|g' ${BUILD_DIR}/pom.xml
COPY ems-nebulous-translator   ${BUILD_DIR}/ems-nebulous-translator
RUN mvn -B -ntp -rf :ems-nebulous-translator-plugin -DskipTests \
    -Ddocker.image-nebulous=${DOCKER_IMAGE} \
    -Dbuild.description="${BUILD_DESCR}" \
    clean install -P '!build-docker-image'


# -----------------   EMS Server with Nebulous Translator image   -----------------
FROM $EMS_CORE_IMAGE AS ems-server-with-nebulous-translator

ARG BUILD_DIR=/build

ENV EXTRA_LOADER_PATHS=/plugins/* \
    SCAN_PACKAGES=eu.nebulous.ems \
    IP_SETTING=DEFAULT_IP         \
    SELF_HEALING_ENABLED=false

COPY --from=ems-nebulous-translator-builder ${BUILD_DIR}/ems-nebulous-translator/target/ems-nebulous-*-jar-with-dependencies.jar /plugins/
COPY --from=ems-nebulous-translator-builder ${BUILD_DIR}/ems-nebulous-translator/target/banner.txt /tmp/

RUN cat /tmp/banner.txt >> ${BASEDIR}/BOOT-INF/classes/banner.txt
