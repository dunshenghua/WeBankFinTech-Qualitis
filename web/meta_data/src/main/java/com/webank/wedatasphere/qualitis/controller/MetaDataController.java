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

package com.webank.wedatasphere.qualitis.controller;

import com.google.common.collect.Maps;
import com.webank.wedatasphere.qualitis.config.LinkisConfig;
import com.webank.wedatasphere.qualitis.constants.ResponseStatusConstants;
import com.webank.wedatasphere.qualitis.exception.PermissionDeniedRequestException;
import com.webank.wedatasphere.qualitis.exception.UnExpectedRequestException;
import com.webank.wedatasphere.qualitis.metadata.client.OperateCiService;
import com.webank.wedatasphere.qualitis.metadata.exception.MetaDataAcquireFailedException;
import com.webank.wedatasphere.qualitis.metadata.request.GetUserColumnByCsRequest;
import com.webank.wedatasphere.qualitis.metadata.request.GetUserTableByCsIdRequest;
import com.webank.wedatasphere.qualitis.metadata.response.*;
import com.webank.wedatasphere.qualitis.metadata.response.cluster.ClusterInfoDetail;
import com.webank.wedatasphere.qualitis.metadata.response.column.ColumnInfoDetail;
import com.webank.wedatasphere.qualitis.metadata.response.db.DbInfoDetail;
import com.webank.wedatasphere.qualitis.metadata.response.table.CsTableInfoDetail;
import com.webank.wedatasphere.qualitis.metadata.response.table.TableInfoDetail;
import com.webank.wedatasphere.qualitis.project.request.CommonChecker;
import com.webank.wedatasphere.qualitis.request.*;
import com.webank.wedatasphere.qualitis.response.*;
import com.webank.wedatasphere.qualitis.rule.entity.Rule;
import com.webank.wedatasphere.qualitis.rule.service.FpsService;
import com.webank.wedatasphere.qualitis.rule.service.LinkisDataSourceService;
import com.webank.wedatasphere.qualitis.service.FileService;
import com.webank.wedatasphere.qualitis.service.MetaDataService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.ResourceAccessException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author howeye
 */
@Path("api/v1/projector/meta_data")
public class MetaDataController {

    @Autowired
    private OperateCiService operateCiService;
    @Autowired
    private FileService fileService;
    @Autowired
    private FpsService fpsService;
    @Autowired
    private LinkisConfig linkisConfig;
    @Autowired
    private MetaDataService metaDataService;
    @Autowired
    private LinkisDataSourceService linkisDataSourceService;

    private static final Logger LOGGER = LoggerFactory.getLogger(MetaDataController.class);

    @FunctionalInterface
    private interface MetaDataAction<T> {
        GeneralResponse<T> execute() throws Exception;
    }

    private <T> GeneralResponse<T> handleRequest(MetaDataAction<T> action, String errorMsgKey) throws UnExpectedRequestException, PermissionDeniedRequestException {
        try {
            return action.execute();
        } catch (UnExpectedRequestException e) {
            throw e;
        } catch (PermissionDeniedRequestException e) {
            throw e;
        } catch (MetaDataAcquireFailedException e) {
            LOGGER.error("Failed to process metadata request, caused by: {}", e.getMessage(), e);
            return new GeneralResponse<>(ResponseStatusConstants.SERVER_ERROR, e.getMessage(), null);
        } catch (ResourceAccessException e) {
            LOGGER.error("Failed to access third-party service, caused by: {}", e.getMessage(), e);
            return new GeneralResponse<>(ResponseStatusConstants.SERVER_ERROR, "{&PARAMS_ERROR_FOR_THIRD_PART_SERVICE}", null);
        } catch (Exception e) {
            LOGGER.error("Failed to process metadata request, caused by: {}", e.getMessage(), e);
            return new GeneralResponse<>(ResponseStatusConstants.SERVER_ERROR, errorMsgKey, null);
        }
    }

    // ==================== Hive Metadata ====================

    @POST
    @Path("cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllClusterResponse<ClusterInfoDetail>> getUserCluster(GetUserClusterRequest request) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> metaDataService.getUserCluster(request), "{&FAILED_TO_GET_CLUSTER}");
    }

    @POST
    @Path("db")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<DbInfoDetail>> getUserDbByCluster(GetUserDbByClusterRequest request) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> metaDataService.getUserDbByCluster(request), "{&FAILED_TO_GET_DATABASE_BY_CLUSTER}");
    }

    @POST
    @Path("table")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<TableInfoDetail>> getUserTableByDbId(GetUserTableByDbIdRequest request) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> metaDataService.getUserTableByDbId(request), "{&FAILED_TO_GET_TABLE_BY_DATABASE}");
    }

    @POST
    @Path("cs_table")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<CsTableInfoDetail>> getContextServiceTableByCsId(GetUserTableByCsIdRequest request) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> metaDataService.getUserTableByCsId(request), "{&FAILED_TO_GET_TABLE_BY_CSID}");
    }

    @POST
    @Path("column")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<ColumnInfoDetail>> getUserColumnByTableId(GetUserColumnByTableIdRequest request) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> metaDataService.getUserColumnByTableId(request), "{&FAILED_TO_GET_COLUMN_BY_TABLE}");
    }

    @POST
    @Path("cs_column")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<ColumnInfoDetail>> getUserColumnByContextService(GetUserColumnByCsRequest request) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> metaDataService.getUserColumnByCsId(request), "{&FAILED_TO_GET_COLUMN_BY_TABLE}");
    }

    // ==================== Multi-DB Rules ====================

    @POST
    @Path("mul_db")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<MulDbResponse> addMultiDbRules(MulDbRequest request) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            String dbs = metaDataService.addMultiDbRules(request);
            return new GeneralResponse<>(ResponseStatusConstants.OK, "{&SUCCESS_MUL_DBS_COMPARE}", new MulDbResponse(dbs));
        }, "{&FAILED_TO_ADD_MULTI_SOURCE_RULE}");
    }

    // ==================== SubSystem / Product ====================

    @POST
    @Path("subSystemInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<SubSystemResponse>> getSubSystemInfo() throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_SUB_SYSTEM_INFO_SUCCESS}", operateCiService.getAllSubSystemInfo()), "{&FAILED_TO_GET_SUB_SYSTEM_INFO_CMDB}");
    }

    @POST
    @Path("productInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<ProductResponse>> getProductInfo() throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_PRODUCT_INFO_SUCCESS}", operateCiService.getAllProductInfo()), "{&FAILED_TO_GET_PRODUCT_INFO_CMDB}");
    }

    // ==================== Department / DevOps ====================

    @POST
    @Path("system/departmentInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<CmdbDepartmentResponse>> getAllDepartmentBySourceType() throws UnExpectedRequestException {
        return new GeneralResponse<>(ResponseStatusConstants.OK, "success", metaDataService.findAllDepartment(null));
    }

    @GET
    @Path("system/devAndOpsInfo/{deptCode}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<DepartmentSubResponse>> getSubDepartmentInfoBySourceType(@PathParam("deptCode") Integer deptCode) throws UnExpectedRequestException {
        return new GeneralResponse<>(ResponseStatusConstants.OK, "success", metaDataService.getSubDepartmentByDeptCode(null, deptCode));
    }

    @POST
    @Path("system/departmentInfoWithRole")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<CmdbDepartmentResponse>> getDepartmentInfoListByRoleType() throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_DEPARTMENT_INFO_SUCCESS}", metaDataService.getDepartmentInfoListByRoleType(null)), "{&FAILED_TO_GET_DEPARTMENT_INFO}");
    }

    @GET
    @Path("system/devAndOpsInfoWithRole/{deptCode}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<DepartmentSubResponse>> getDevAndOpsInfoByRoleTypeAndSourceType(@PathParam("deptCode") Integer deptCode) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            if (deptCode == null) {
                throw new UnExpectedRequestException("Dept code {&CAN_NOT_BE_NULL_OR_EMPTY}");
            }
            return new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_DEPARTMENT_INFO_SUCCESS}", metaDataService.getDevAndOpsInfoListByRoleType(null, deptCode));
        }, "{&FAILED_TO_GET_DEPARTMENT_INFO}");
    }

    // ==================== DataMap Queries ====================

    @GET
    @Path("datamap/database")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> getDbFromDatamap(@QueryParam("search_key") String searchKey, @QueryParam("cluster_name") String clusterName
        , @QueryParam("proxy_user") String proxyUser) {
        try {
            String id = metaDataService.getDbFromDatamap(searchKey, clusterName, proxyUser);
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(1);
            map.put("db_id", id);
            return new GeneralResponse<>(ResponseStatusConstants.OK, "{&SUCCESS_TO_GET_DATABASE_FROM_DATAMAP}", map);
        } catch (Exception e) {
            LOGGER.error("Failed to get database info from DataMap.", e);
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(1);
            map.put("db_id", "");
            return new GeneralResponse<>(ResponseStatusConstants.OK, "{&FAILED_TO_GET_DATABASE_FROM_DATAMAP}", map);
        }
    }

    @GET
    @Path("datamap/table")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> getTableFromDatamap(@QueryParam("db_id") String dbId, @QueryParam("dataset_name") String datasetName
        , @QueryParam("cluster_name") String clusterName, @QueryParam("proxy_user") String proxyUser) {
        try {
            Integer id  = metaDataService.getDatasetFromDatamap(dbId, datasetName, clusterName, proxyUser);
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(1);
            map.put("dataset_id", id);
            return new GeneralResponse<>(ResponseStatusConstants.OK, "{&SUCCESS_TO_GET_TABLE_FROM_DATAMAP}", map);
        } catch (Exception e) {
            LOGGER.error("Failed to get table info from DataMap.", e);
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(1);
            map.put("dataset_id", "");
            return new GeneralResponse<>(ResponseStatusConstants.OK, "{&FAILED_TO_GET_TABLE__FROM_DATAMAP}", map);
        }
    }

    @GET
    @Path("datamap/column")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> getColumnFromDatamap(@QueryParam("dataset_id") Long datasetId, @QueryParam("field_name") String fieldName
        , @QueryParam("proxy_user") String proxyUser) {
        try {
            Map<String, Object> response = metaDataService.getColumnFromDatamap(datasetId,fieldName, proxyUser);
            String id = (String) ((List<Map<String, Object>>) response.get("content")).iterator().next().get("stdCode");
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(1);
            map.put("std_code", id);
            return new GeneralResponse<>(ResponseStatusConstants.OK, "{&SUCCESS_TO_GET_COLUMN_FROM_DATAMAP}", map);
        } catch (Exception e) {
            LOGGER.error("Failed to get column info from DataMap.", e);
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(1);
            map.put("std_code", "");
            return new GeneralResponse<>(ResponseStatusConstants.OK, "{&FAILED_TO_GET_COLUMN_FROM_DATAMAP}", map);
        }
    }

    @GET
    @Path("datamap/standard")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> getStandardFromDatamap(@QueryParam("std_code") String stdCode, @QueryParam("source") String source
        , @QueryParam("proxy_user") String proxyUser) {
        try {
            Map<String, Object> response = metaDataService.getDataStandardDetailFromDatamap(stdCode, source, proxyUser);
            String checkRule = (String) response.get("checkRule");
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(1);
            map.put("check_rule", checkRule);
            return new GeneralResponse<>(ResponseStatusConstants.OK, "{&SUCCESS_TO_GET_STANDARD_FROM_DATAMAP}", map);
        } catch (Exception e) {
            LOGGER.error("Failed to get standard info from DataMap.", e);
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(1);
            map.put("check_rule", "");
            return new GeneralResponse<>(ResponseStatusConstants.OK, "{&FAILED_TO_GET_STANDARD_FROM_DATAMAP}", map);
        }
    }

    // ==================== Data Source Management ====================

    @GET
    @Path("data_source/types/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> getAllDataSourceTypes(@QueryParam("proxyUser") String proxyUser) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            String clusterName = linkisConfig.getDatasourceCluster();
            return metaDataService.getAllDataSourceTypes(clusterName, proxyUser);
        }, "Failed");
    }

    @GET
    @Path("data_source/env")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> getDataSourceEnv(@QueryParam("proxyUser") String proxyUser) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            String clusterName = linkisConfig.getDatasourceCluster();
            return metaDataService.getDataSourceEnv(clusterName, proxyUser);
        }, "Failed");
    }

    @GET
    @Path("data_source/env/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<DataSourceEnvResponse>> envList(@QueryParam("dataSourceId") Long dataSourceId
                                                            , @QueryParam("dcnRangeType") String dcnRangeType) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            String clusterName = linkisConfig.getDatasourceCluster();
            return metaDataService.getEnvList(clusterName, dataSourceId, dcnRangeType);
        }, "Failed");
    }

    @POST
    @Path("data_source/info/advance")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse getDataSourceAdvanceInfo(GetDataSourceRequest request) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> metaDataService.getDataSourceInfoWithAdvance(request), "Failed");
    }

    @GET
    @Path("data_source/versions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> getDataSourceVersions(@QueryParam("proxyUser") String proxyUser
    , @QueryParam("dataSourceId") Long dataSourceId) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            if (dataSourceId == null) {
                throw new UnExpectedRequestException("Data source ID" + "{&CAN_NOT_BE_NULL_OR_EMPTY}");
            }
            String clusterName = linkisConfig.getDatasourceCluster();
            return metaDataService.getDataSourceVersions(clusterName, proxyUser, dataSourceId);
        }, "Failed");
    }

    @GET
    @Path("data_source/info")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> getDataSourceInfoPage(@QueryParam("currentPage") Integer currentPage, @QueryParam("pageSize") Integer pageSize, @QueryParam("name") String searchName
            , @QueryParam("typeId") Long typeId, @QueryParam("typeName") String typeName) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            GetDataSourceRequest request = new GetDataSourceRequest();
            request.setName(searchName);
            request.setDataSourceTypeId(typeId);
            request.setDataSourceTypeName(typeName);
            request.setPage(currentPage);
            request.setSize(pageSize);
            return metaDataService.getDataSourceInfoWithAdvance(request);
        }, "Failed");
    }

    @GET
    @Path("data_source/info/detail")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> getDataSourceInfoDetail(@QueryParam("proxyUser") String proxyUser
    , @QueryParam("dataSourceId") Long dataSourceId, @QueryParam("versionId") Long versionId) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            if (dataSourceId == null) {
                throw new UnExpectedRequestException("Data source ID or version ID " + "{&CAN_NOT_BE_NULL_OR_EMPTY}");
            }
            String clusterName = linkisConfig.getDatasourceCluster();
            return metaDataService.getDataSourceInfoDetail(clusterName, proxyUser, dataSourceId, versionId);
        }, "Failed");
    }

    @GET
    @Path("data_source/key_define/type")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> getDataSourceKeyDefine(@QueryParam("proxyUser") String proxyUser, @QueryParam("keyId") Long keyId) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            if (keyId == null) {
                throw new UnExpectedRequestException("Key ID " + "{&CAN_NOT_BE_NULL_OR_EMPTY}");
            }
            String clusterName = linkisConfig.getDatasourceCluster();
            return metaDataService.getDataSourceKeyDefine(clusterName, proxyUser, keyId);
        }, "Failed");
    }

    @POST
    @Path("data_source/connect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> connectDataSource(@QueryParam("proxyUser") String proxyUser, @RequestBody DataSourceConnectRequest request) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            if (request == null) {
                throw new UnExpectedRequestException("Request " + "{&CAN_NOT_BE_NULL_OR_EMPTY}");
            }
            String clusterName = linkisConfig.getDatasourceCluster();
            return metaDataService.connectDataSource(clusterName, proxyUser, request);
        }, "{&CONNECT_FAILED}");
    }

    @POST
    @Path("data_source/publish")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> publishDataSource(@QueryParam("proxyUser") String proxyUser
        , @QueryParam("dataSourceId") Long dataSourceId, @QueryParam("versionId") Long versionId) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            if (dataSourceId == null || versionId == null) {
                throw new UnExpectedRequestException("Request " + "{&CAN_NOT_BE_NULL_OR_EMPTY}");
            }
            String clusterName = linkisConfig.getDatasourceCluster();
            return metaDataService.publishDataSource(clusterName, proxyUser, dataSourceId, versionId);
        }, "Failed");
    }

    @POST
    @Path("data_source/expire")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> expireDataSource(@QueryParam("proxyUser") String proxyUser
        , @QueryParam("dataSourceId") Long dataSourceId) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            if (dataSourceId == null) {
                throw new UnExpectedRequestException("Request " + "{&CAN_NOT_BE_NULL_OR_EMPTY}");
            }
            String clusterName = linkisConfig.getDatasourceCluster();
            return metaDataService.expireDataSource(clusterName, proxyUser, dataSourceId);
        }, "Failed");
    }

    @POST
    @Path("data_source/modify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse modifyDataSource(@QueryParam("dataSourceId") Long dataSourceId, DataSourceModifyRequest request) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            if (request == null || dataSourceId == null) {
                throw new UnExpectedRequestException("Request " + "{&CAN_NOT_BE_NULL_OR_EMPTY}");
            }
            String clusterName = linkisConfig.getDatasourceCluster();
            return metaDataService.modifyDataSource(clusterName, dataSourceId, request);
        }, "Failed");
    }

    @POST
    @Path("data_source/param/modify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse modifyDataSourceParam(@QueryParam("dataSourceId") Long dataSourceId, @RequestBody DataSourceParamModifyRequest request) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            if (request == null || dataSourceId == null) {
                throw new UnExpectedRequestException("Request " + "{&CAN_NOT_BE_NULL_OR_EMPTY}");
            }
            String clusterName = linkisConfig.getDatasourceCluster();
            return metaDataService.modifyDataSourceParam(clusterName, dataSourceId, request);
        }, "Failed");
    }

    @POST
    @Path("data_source/create")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse createDataSource(@RequestBody DataSourceModifyRequest request) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            if (request == null) {
                throw new UnExpectedRequestException("Request " + "{&CAN_NOT_BE_NULL_OR_EMPTY}");
            }
            String clusterName = linkisConfig.getDatasourceCluster();
            return metaDataService.createDataSource(clusterName, request);
        }, "Failed");
    }

    @GET
    @Path("data_source/dbs")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<DbInfoDetail>> getDbsByDataSource(@QueryParam("proxyUser") String proxyUser
        , @QueryParam("dataSourceId") Long dataSourceId, @QueryParam("envId") Long envId) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            if (dataSourceId == null) {
                throw new UnExpectedRequestException("Request " + "{&CAN_NOT_BE_NULL_OR_EMPTY}");
            }
            String clusterName = linkisConfig.getDatasourceCluster();
            Map<String, Object> response = metaDataService.getDbsByDataSource(clusterName, proxyUser, dataSourceId, envId);
            GetAllResponse allResponse = new GetAllResponse();
            List<String> dbs = (List<String>) response.get("dbs");
            List<DbInfoDetail> dbInfoDetails = new ArrayList<>(CollectionUtils.isEmpty(dbs) ? 0 : dbs.size());
            dbs = dbs.stream().distinct().collect(Collectors.toList());
            for (String db : dbs) {
                DbInfoDetail dbInfoDetail = new DbInfoDetail(db);
                dbInfoDetails.add(dbInfoDetail);
            }
            allResponse.setTotal(CollectionUtils.isEmpty(dbInfoDetails) ? 0 : dbInfoDetails.size());
            allResponse.setData(dbInfoDetails);
            return new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_DB_SUCCESSFULLY}", allResponse);
        }, "{&FAILED_TO_GET_DATABASE_BY_CLUSTER}");
    }

    @GET
    @Path("data_source/tables")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<TableInfoDetail>> getTablesByDataSource(@QueryParam("proxyUser") String proxyUser
        , @QueryParam("dataSourceId") Long dataSourceId, @QueryParam("envId") Long envId, @QueryParam("dbName") String dbName) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            if (dataSourceId == null || StringUtils.isBlank(dbName)) {
                throw new UnExpectedRequestException("Request " + "{&CAN_NOT_BE_NULL_OR_EMPTY}");
            }
            String clusterName = linkisConfig.getDatasourceCluster();
            Map<String, Object> response = metaDataService.getTablesByDataSource(clusterName, proxyUser, dataSourceId, dbName, envId);
            GetAllResponse allResponse = new GetAllResponse();
            List<String> tables = (List<String>) response.get("tables");
            List<TableInfoDetail> tableInfoDetails = new ArrayList<>(CollectionUtils.isEmpty(tables) ? 0 : tables.size());
            for (String table : tables) {
                TableInfoDetail tableInfoDetail = new TableInfoDetail(table);
                tableInfoDetails.add(tableInfoDetail);
            }
            allResponse.setTotal(CollectionUtils.isEmpty(tableInfoDetails) ? 0 : tableInfoDetails.size());
            allResponse.setData(tableInfoDetails);
            return new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_TABLE_SUCCESSFULLY}", allResponse);
        }, "{&FAILED_TO_GET_TABLE_BY_DATABASE}");
    }

    @GET
    @Path("data_source/columns")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<ColumnInfoDetail>> getColumnsByDataSource(@QueryParam("proxyUser") String proxyUser
        , @QueryParam("dataSourceId") Long dataSourceId, @QueryParam("envId") Long envId, @QueryParam("dbName") String dbName, @QueryParam("tableName") String tableName) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            if (dataSourceId == null || StringUtils.isBlank(dbName) || StringUtils.isBlank(tableName)) {
                throw new UnExpectedRequestException("Request " + "{&CAN_NOT_BE_NULL_OR_EMPTY}");
            }
            String clusterName = linkisConfig.getDatasourceCluster();
            return metaDataService.getColumnsByDataSource(clusterName, proxyUser, dataSourceId, dbName, tableName, envId);
        }, "{&FAILED_TO_GET_COLUMN_BY_TABLE}");
    }

    // ==================== UDF Management ====================

    @POST
    @Path("udf/upload")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public GeneralResponse<String> uploadFile(@FormDataParam("file") InputStream fileInputStream, @FormDataParam("file") FormDataContentDisposition fileDisposition) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> fileService.uploadFile(fileInputStream, fileDisposition, ""), "{&FAILED_TO_UPLOAD_FILE}");
    }

    @GET
    @Path("udf/directory")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<String>> getDirectory(@QueryParam("category") String category, @QueryParam("cluster_name") String clusterName) throws UnExpectedRequestException, MetaDataAcquireFailedException, PermissionDeniedRequestException {
        return handleRequest(() -> metaDataService.getDirectory(category, clusterName), "{&FAILED_TO_GET_CATEGORY_LIST}");
    }

    @POST
    @Path("udf/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<UdfResponse> addUdf(UdfRequest udfRequest) throws UnExpectedRequestException, PermissionDeniedRequestException, MetaDataAcquireFailedException {
        return handleRequest(() -> metaDataService.addUdf(udfRequest), "{&FAILED_TO_ADD_UDF}");
    }

    @POST
    @Path("udf/modify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<UdfResponse> modifyUdf(UdfRequest udfRequest) throws UnExpectedRequestException, PermissionDeniedRequestException, MetaDataAcquireFailedException {
        return handleRequest(() -> metaDataService.modifyUdf(udfRequest), "{&FAILED_TO_MODIFY_UDF}");
    }

    @GET
    @Path("udf/detail")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<UdfResponse> getUdfDetail(@QueryParam("id") Long id) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> metaDataService.getUdfDetail(id), "{&FAILED_TO_GET_UDF}");
    }

    @POST
    @Path("udf/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<DataInfo<UdfResponse>> getUdfAllWithPage(UdfRequest udfRequest) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> metaDataService.getUdfAllWithPage(udfRequest), "{&FAILED_TO_GET_UDF}");
    }

    @POST
    @Path("udf/delete")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<UdfResponse> deleteUdf(UdfRequest udfRequest) throws UnExpectedRequestException, PermissionDeniedRequestException, MetaDataAcquireFailedException {
        return handleRequest(() -> metaDataService.deleteUdf(udfRequest), "{&FAILED_TO_DELETE_UDF}");
    }

    @GET
    @Path("udf/switch")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<UdfResponse> switchUdfStatus(@QueryParam("id") Long id, @QueryParam("is_load") Boolean isLoad) throws UnExpectedRequestException, PermissionDeniedRequestException, MetaDataAcquireFailedException {
        return handleRequest(() -> metaDataService.switchUdfStatus(id, isLoad), "{&FAILED_TO_MODIFY_UDF}");
    }

    // ==================== DCN / Misc ====================

    @POST
    @Path("dcn")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse getDcn(GetDcnRequest getDcnRequest) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return handleRequest(() -> {
            CommonChecker.checkObject(getDcnRequest.getSubSystemId(), "sub_system_id");
            CommonChecker.checkString(getDcnRequest.getDcnRangeType(), "dcn_range_type");
            Object result = metaDataService.getDcnList(getDcnRequest.getSubSystemId(), getDcnRequest.getDcnRangeType(), Collections.emptyList());
            return new GeneralResponse(ResponseStatusConstants.OK, "success", result);
        }, "Failed to get dcn tdsql info.");
    }

    @GET
    @Path("all/dataSourceName")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse getDataSourceNameList() {
        return new GeneralResponse(ResponseStatusConstants.OK, "success", metaDataService.getDataSourceNameList());
    }

    @GET
    @Path("data_source/check_related_rules")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse checkRulesIfRelatedTo(@QueryParam("dataSourceId") Long dataSourceId) {
        List<Rule> rules = metaDataService.getRulesRelatedTo(dataSourceId);
        if (CollectionUtils.isNotEmpty(rules)) {
            List<String> ruleNameList = rules.stream().map(Rule::getName).collect(Collectors.toList());
            return new GeneralResponse(ResponseStatusConstants.OK, "There are some rules related to the DataSource.", ruleNameList);
        }
        return new GeneralResponse(ResponseStatusConstants.OK, "There are not rules related to the DataSource.", null);
    }

}
