#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#ARG_POSITIONAL_INF([scenario], [The scenario that you want to running])
#DEFINE_SCRIPT_DIR([AGENT_TEST_PLUGIN_HOME])
#ARG_HELP()
#ARGBASH_GO
# [
SCENARIO_PACKAGES_TARGET_DIRECTORY=${AGENT_TEST_PLUGIN_HOME}/dist
SCENARIO_CASES_HOME=${AGENT_TEST_PLUGIN_HOME}/testcase && mkdir -p ${SCENARIO_CASES_HOME}
MVN_EXEC=${AGENT_TEST_PLUGIN_HOME}/../../../mvnw
AGENT_HOME=${AGENT_TEST_PLUGIN_HOME}/../../../skywalking-agent

START_TIME=$(date +"%Y-%m-%d %H:%M:%S")
function clearResources(){
    docker ps -a | grep skywalking-agent-test | awk '{print $1}' | xargs docker rm -f
    #rm -rf $SCENARIO_CASES_HOME
}

UNPACKED_VERSION=()
for SCENARIO in ${_arg_scenario[@]}
do
  # check if the scenario directory is existing
  SCENARIO_HOME=${AGENT_TEST_PLUGIN_HOME}/${SCENARIO}
  #
  SUPPORT_VERSION_FILE=${SCENARIO_HOME}/support-version.list
  SUPPORT_VERSIONS=($(grep -v -e "^$" $SUPPORT_VERSION_FILE | grep -v "#"))
  echo "Support version: ${SUPPORT_VERSIONS[@]}"

  MVN_PROFILES=""
  for SCENARIO_VERSION in ${SUPPORT_VERSIONS[@]}
  do
     cd ${SCENARIO_HOME} && ${MVN_EXEC} clean write-text-files:write-text-files package -P ${SCENARIO}-${SCENARIO_VERSION}
  done

  for SCENARIO_VERSION in ${SUPPORT_VERSIONS[@]}
  do
    # check if the scenario package is exist. if not. record it and then throw an exception

    tar -zxvf ${SCENARIO_PACKAGES_TARGET_DIRECTORY}/${SCENARIO}-${SCENARIO_VERSION}.tar.gz -C ${SCENARIO_CASES_HOME} > /dev/null 2>&1
    bash ${SCENARIO_CASES_HOME}/${SCENARIO}-${SCENARIO_VERSION}/scenario.sh ${SCENARIO_VERSION} ${AGENT_HOME}
  done
done

TESTCASE_COUNT=$(find $SCENARIO_PACKAGES_TARGET_DIRECTORY -name *.tar.gz | wc -l | tr -s [:space:])
for ((i=1;i<=1000;i++));
do
    FINISHED_COUNT=$(find $SCENARIO_CASES_HOME -name actualData.yaml | wc -l | tr -s [:space:])
    if [[ "$TESTCASE_COUNT" == "$FINISHED_COUNT" ]]; then
        ## RUNNING validate
        echo "START TIME: $START_TIME, END TIME: $(date +"%Y-%m-%d %H:%M:%S")"
        clearResources
        exit 0
    fi
    sleep 10
done
echo `date +"%Y-%m-%d %H:%M:%S"`
# clear resource
clearResources
# ]