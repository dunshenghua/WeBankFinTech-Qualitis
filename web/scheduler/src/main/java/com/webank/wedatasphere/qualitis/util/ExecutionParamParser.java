/*
 * Copyright 2019 WeBank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.qualitis.util;

import com.google.common.collect.Maps;
import com.webank.wedatasphere.qualitis.constant.SpecCharEnum;
import com.webank.wedatasphere.qualitis.constants.QualitisConstants;
import com.webank.wedatasphere.qualitis.request.BaseExecutionRequest;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified parser for execution parameters.
 * Extracts the common parsing pipeline previously duplicated across projectExecution,
 * groupExecution, ruleListExecution, and dataSourceExecution in OuterExecutionServiceImpl.
 *
 * Pipeline:
 * 1. Extract set_flag entries (qualitis.spark.set.*) from executionParam
 * 2. Merge set_flag with existing setFlag on the request
 * 3. Extract fps_id, fps_hash, env_names from executionParam
 * 4. Append standalone splitBy and engineReuse into executionParam
 * 5. Parse partition, run_date, run_today, split_by from the final executionParam
 *
 * @author refactored
 */
public class ExecutionParamParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionParamParser.class);

    private static final String EXECUTION_PARAM = "execution_param";
    private static final String SET_FLAG = "set_flag";
    private static final String FPS_ID = "fps_id";
    private static final String FPS_HASH = "fps_hash";
    private static final String ENV_NAMES = "env_names";

    private static final String PARTITION = "partition";
    private static final String RUN_DATE = "run_date";
    private static final String RUN_TODAY = "run_today";
    private static final String SPLIT_BY = "split_by";
    private static final String ENGINE_REUSE = "engine_reuse";

    private ExecutionParamParser() {
        // Utility class
    }

    /**
     * Parse all execution parameters from the given request.
     * This method does NOT modify the request; it returns a new result object.
     *
     * @param request the execution request containing raw parameters
     * @return parsed result containing cleaned executionParam, setFlag, fps fields, partition info, etc.
     */
    public static ParsedExecParams parse(BaseExecutionRequest request) {
        String executionParam = request.getExecutionParam();
        String setFlag = request.getSetFlag();
        String fpsFileId = request.getFpsFileId();
        String fpsHashValue = request.getFpsHashValue();
        String envNames = request.getEnvNames();

        LOGGER.info("Execute parameter entry. execution_param: {}", executionParam);

        // Step 1: Extract set_flag entries (qualitis.spark.set.*) from executionParam
        Map<String, Object> setFlagResult = handleSetFlagParameters(executionParam);
        if (!setFlagResult.isEmpty()) {
            if (setFlagResult.get(EXECUTION_PARAM) != null) {
                executionParam = setFlagResult.get(EXECUTION_PARAM).toString();
            }
            if (setFlagResult.get(SET_FLAG) != null && StringUtils.isBlank(setFlag)) {
                setFlag = setFlagResult.get(SET_FLAG).toString();
            } else if (setFlagResult.get(SET_FLAG) != null && StringUtils.isNotBlank(setFlag)) {
                setFlag = mergeSetFlag(setFlag, setFlagResult.get(SET_FLAG).toString());
            }
        }
        LOGGER.info("set_flag: {}", setFlag);

        // Step 2: Extract fps_id, fps_hash, env_names from executionParam
        Map<String, Object> fpsResult = handleFpsIdAndValueParameters(executionParam, fpsFileId, fpsHashValue);
        if (!fpsResult.isEmpty()) {
            if (fpsResult.get(EXECUTION_PARAM) != null) {
                executionParam = fpsResult.get(EXECUTION_PARAM).toString();
            }
            if (fpsResult.get(FPS_ID) != null) {
                fpsFileId = fpsResult.get(FPS_ID).toString();
            }
            if (fpsResult.get(FPS_HASH) != null) {
                fpsHashValue = fpsResult.get(FPS_HASH).toString();
            }
            if (fpsResult.get(ENV_NAMES) != null) {
                envNames = fpsResult.get(ENV_NAMES).toString();
            }
        }
        LOGGER.info("fps_file_id: {}", fpsFileId);
        LOGGER.info("fps_hash: {}", fpsHashValue);
        LOGGER.info("env_names: {}", envNames);

        // Step 3: Build lastExecutionParam by appending standalone splitBy and engineReuse
        String lastExecutionParam = buildLastExecutionParam(executionParam, request.getSplitBy(), request.getEngineReuse());

        // Step 4: Parse partition, run_date, run_today, split_by
        StringBuilder partition = new StringBuilder();
        StringBuilder runDate = new StringBuilder();
        StringBuilder runToday = new StringBuilder();
        StringBuilder splitBy = new StringBuilder();
        Map<String, String> execParamMap = new HashMap<>(5);

        parseExecParams(partition, runDate, runToday, splitBy, lastExecutionParam, execParamMap);

        // Build result
        ParsedExecParams result = new ParsedExecParams();
        result.setExecutionParam(executionParam);
        result.setSetFlag(setFlag);
        result.setFpsFileId(fpsFileId);
        result.setFpsHashValue(fpsHashValue);
        result.setEnvNames(envNames);
        result.setPartition(partition);
        result.setRunDate(runDate);
        result.setRunToday(runToday);
        result.setSplitBy(splitBy);
        result.setExecParamMap(execParamMap);
        return result;
    }

    /**
     * Merge existing setFlag with newly parsed setFlag.
     * Strips "spark.sql." prefix from existing entries before appending new ones.
     */
    private static String mergeSetFlag(String existingSetFlag, String newSetFlag) {
        StringBuilder tmpSetFlag = new StringBuilder();
        String[] setStrs = existingSetFlag.split(SpecCharEnum.DIVIDER.getValue());
        for (String setStr : setStrs) {
            if (setStr.startsWith("spark.sql.")) {
                tmpSetFlag.append(setStr.replace("spark.sql.", "")).append(SpecCharEnum.DIVIDER.getValue());
            }
        }

        if (tmpSetFlag.length() > 0) {
            return tmpSetFlag.deleteCharAt(tmpSetFlag.length() - 1).toString()
                + SpecCharEnum.DIVIDER.getValue() + newSetFlag;
        } else {
            return existingSetFlag + SpecCharEnum.DIVIDER.getValue() + newSetFlag;
        }
    }

    /**
     * Extract qualitis.spark.set.* entries from executionParam into a set_flag string.
     * Returns a map with keys "execution_param" (remaining params) and "set_flag" (extracted flags).
     */
    private static Map<String, Object> handleSetFlagParameters(String executionParam) {
        Map<String, Object> maps = Maps.newHashMap();

        if (StringUtils.isNotEmpty(executionParam) && executionParam.contains(QualitisConstants.SPARK_SET_FLAG)) {
            StringBuilder setFlag = new StringBuilder();
            StringBuilder tmpExecParams = new StringBuilder();
            String[] setStrs = executionParam.split(SpecCharEnum.DIVIDER.getValue());
            for (String str : setStrs) {
                if (str.startsWith(QualitisConstants.SPARK_SET_FLAG)) {
                    setFlag.append(str.replace(QualitisConstants.SPARK_SET_FLAG, "")
                        .replace(SpecCharEnum.COLON.getValue(), SpecCharEnum.EQUAL.getValue()))
                        .append(SpecCharEnum.DIVIDER.getValue());
                } else {
                    tmpExecParams.append(str).append(SpecCharEnum.DIVIDER.getValue());
                }
            }
            if (StringUtils.isNotEmpty(setFlag.toString())) {
                maps.put(SET_FLAG, setFlag.deleteCharAt(setFlag.length() - 1).toString());
            }
            if (StringUtils.isNotEmpty(tmpExecParams.toString())) {
                maps.put(EXECUTION_PARAM, tmpExecParams.deleteCharAt(tmpExecParams.length() - 1).toString());
            }
            LOGGER.info("Parsed set flag: {}", maps.get(SET_FLAG));
            LOGGER.info("Remaining execution param: {}", maps.get(EXECUTION_PARAM));
        }
        return maps;
    }

    /**
     * Extract fps_id, fps_hash, env_names from executionParam.
     * Returns a map with extracted values and remaining execution_param.
     */
    private static Map<String, Object> handleFpsIdAndValueParameters(String executionParam, String fpsFileId, String fpsHashValue) {
        Map<String, Object> maps = Maps.newHashMap();
        if (StringUtils.isNotEmpty(executionParam) && executionParam.contains(FPS_ID) && StringUtils.isEmpty(fpsFileId)) {
            StringBuilder fpsId = new StringBuilder();
            StringBuilder tmpExecParams = new StringBuilder();
            String[] setStrs = executionParam.split(SpecCharEnum.DIVIDER.getValue());
            for (String str : setStrs) {
                if (str.startsWith(FPS_ID)) {
                    fpsId.append(str.replace(FPS_ID, "").replace(SpecCharEnum.COLON.getValue(), ""));
                } else {
                    tmpExecParams.append(str).append(SpecCharEnum.DIVIDER.getValue());
                }
            }

            if (StringUtils.isNotEmpty(fpsId.toString())) {
                maps.put(FPS_ID, fpsId.toString());
            }

            if (StringUtils.isNotEmpty(tmpExecParams.toString())) {
                maps.put(EXECUTION_PARAM, tmpExecParams.deleteCharAt(tmpExecParams.length() - 1).toString());
            }
        }

        if (!maps.isEmpty()) {
            if (StringUtils.isNotEmpty(maps.get(EXECUTION_PARAM).toString()) && maps.get(EXECUTION_PARAM).toString().contains(FPS_HASH) && StringUtils.isEmpty(fpsHashValue)) {
                StringBuilder fpsHash = new StringBuilder();
                StringBuilder tmpExecParams = new StringBuilder();
                String[] setStrs = maps.get(EXECUTION_PARAM).toString().split(SpecCharEnum.DIVIDER.getValue());
                for (String str : setStrs) {
                    if (str.startsWith(FPS_HASH)) {
                        fpsHash.append(str.replace(FPS_HASH, "").replace(SpecCharEnum.COLON.getValue(), ""));
                    } else {
                        tmpExecParams.append(str).append(SpecCharEnum.DIVIDER.getValue());
                    }
                }

                if (StringUtils.isNotEmpty(fpsHash.toString())) {
                    maps.put(FPS_HASH, fpsHash.toString());
                }

                if (StringUtils.isNotEmpty(tmpExecParams.toString())) {
                    maps.put(EXECUTION_PARAM, tmpExecParams.deleteCharAt(tmpExecParams.length() - 1).toString());
                }
            }

        } else if (StringUtils.isNotEmpty(executionParam)) {
            if (executionParam.contains(FPS_HASH) && StringUtils.isEmpty(fpsHashValue)) {
                StringBuilder fpsHash = new StringBuilder();
                StringBuilder tmpExecParams = new StringBuilder();
                String[] setStrs = executionParam.split(SpecCharEnum.DIVIDER.getValue());
                for (String str : setStrs) {
                    if (str.startsWith(FPS_HASH)) {
                        fpsHash.append(str.replace(FPS_HASH, "").replace(SpecCharEnum.COLON.getValue(), ""));
                    } else {
                        tmpExecParams.append(str).append(SpecCharEnum.DIVIDER.getValue());
                    }
                }

                if (StringUtils.isNotEmpty(fpsHash.toString())) {
                    maps.put(FPS_HASH, fpsHash.toString());
                }

                if (StringUtils.isNotEmpty(tmpExecParams.toString())) {
                    maps.put(EXECUTION_PARAM, tmpExecParams.deleteCharAt(tmpExecParams.length() - 1).toString());
                }
            } else if (executionParam.contains(ENV_NAMES)) {
                StringBuilder envNamesBuilder = new StringBuilder();
                StringBuilder tmpExecParams = new StringBuilder();
                String[] setStrs = executionParam.split(SpecCharEnum.DIVIDER.getValue());
                for (String str : setStrs) {
                    if (str.startsWith(ENV_NAMES)) {
                        envNamesBuilder.append(str.replace(ENV_NAMES, "").replace(SpecCharEnum.COLON.getValue(), ""));
                    } else {
                        tmpExecParams.append(str).append(SpecCharEnum.DIVIDER.getValue());
                    }
                }

                if (StringUtils.isNotEmpty(envNamesBuilder.toString())) {
                    maps.put(ENV_NAMES, envNamesBuilder.toString());
                }

                if (StringUtils.isNotEmpty(tmpExecParams.toString())) {
                    maps.put(EXECUTION_PARAM, tmpExecParams.deleteCharAt(tmpExecParams.length() - 1).toString());
                }
            }
        }

        return maps;
    }

    /**
     * Build the final executionParam string by appending standalone splitBy and engineReuse
     * if they are not already present in executionParam.
     */
    private static String buildLastExecutionParam(String executionParam, String splitBy, Boolean engineReuse) {
        StringBuilder specialSplitBy = new StringBuilder();
        StringBuilder specialEngineReuse = new StringBuilder();
        if (StringUtils.isNotBlank(splitBy) && (executionParam == null || !executionParam.contains(SPLIT_BY))) {
            specialSplitBy.append(SPLIT_BY).append(SpecCharEnum.COLON.getValue()).append(splitBy);
        }
        if (engineReuse != null && (executionParam == null || !executionParam.contains(ENGINE_REUSE))) {
            specialEngineReuse.append(ENGINE_REUSE).append(SpecCharEnum.COLON.getValue()).append(engineReuse);
        }
        StringBuilder lastExecutionParam = new StringBuilder();
        lastExecutionParam.append(StringUtils.isNotEmpty(executionParam) ? executionParam : "");
        if (specialSplitBy.length() > 0) {
            lastExecutionParam.append(StringUtils.isNotBlank(lastExecutionParam.toString())
                ? SpecCharEnum.DIVIDER.getValue() + specialSplitBy.toString()
                : specialSplitBy.toString());
        }
        if (specialEngineReuse.length() > 0) {
            lastExecutionParam.append(StringUtils.isNotBlank(lastExecutionParam.toString())
                ? SpecCharEnum.DIVIDER.getValue() + specialEngineReuse.toString()
                : specialEngineReuse.toString());
        }
        return lastExecutionParam.toString();
    }

    /**
     * Parse partition, run_date, run_today, split_by from the final executionParam string.
     * Also populates execParamMap with all key:value pairs.
     */
    private static void parseExecParams(StringBuilder partition, StringBuilder runDate, StringBuilder runToday,
            StringBuilder splitBy, String execParams, Map<String, String> execParamMap) {
        if (StringUtils.isNotBlank(execParams)) {
            String[] execParamStrs = execParams.split(SpecCharEnum.DIVIDER.getValue());
            for (String str : execParamStrs) {
                String[] strs = str.split(SpecCharEnum.COLON.getValue());
                String execParamKey = strs[0];
                String execParamValue = strs[1];

                if (PARTITION.equals(execParamKey)) {
                    if (partition.length() == 0) {
                        partition.append(execParamValue);
                    } else {
                        partition.append(" and ").append(execParamValue);
                    }
                    continue;
                } else if (RUN_DATE.equals(execParamKey)) {
                    if (runDate.length() == 0) {
                        runDate.append(execParamValue);
                    }
                    continue;
                } else if (SPLIT_BY.equals(execParamKey)) {
                    if (splitBy.length() == 0) {
                        splitBy.append(execParamValue);
                    }
                    continue;
                } else if (RUN_TODAY.equals(execParamKey)) {
                    if (runToday.length() == 0) {
                        runToday.append(execParamValue);
                    }
                    continue;
                }

                execParamMap.put(str.split(SpecCharEnum.COLON.getValue())[0], str.split(SpecCharEnum.COLON.getValue())[1]);
            }
        }
    }
}
