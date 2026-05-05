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

ARG BASE_IMAGE=eclipse-temurin:21.0.10_7-jre-noble

# ----------------- EMS Builder image -----------------
FROM $EMS_CORE_BUILDER_IMAGE AS ems-nebulous-translator-builder

WORKDIR ${BASEDIR}

COPY pom.xml                   ${BASEDIR}/pom.xml
RUN sed -i 's|<module>\.\./ems-main/ems-core</module>|<module>ems-core</module>|g' ${BASEDIR}/pom.xml
COPY ems-nebulous-translator   ${BASEDIR}/ems-nebulous-translator
RUN mvn -rf :ems-nebulous-translator-plugin -DskipTests clean install -P '!build-docker-image'


# -----------------   EMS Server with Nebulous Translator image   -----------------
FROM $EMS_CORE_IMAGE AS ems-server-with-nebulous-translator

ENV EXTRA_LOADER_PATHS=/plugins/* \
    SCAN_PACKAGES=eu.nebulous.ems \
    IP_SETTING=DEFAULT_IP         \
    SELF_HEALING_ENABLED=false

COPY --from=ems-nebulous-translator-builder /app/ems-nebulous-translator/target/ems-nebulous-*-jar-with-dependencies.jar /plugins/

RUN date -Iseconds > /tmp/build.timestamp
