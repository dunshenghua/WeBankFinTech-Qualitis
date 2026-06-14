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
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;

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

    private String getClusterName() {
        return linkisConfig.getDatasourceCluster();
    }

    @POST
    @Path("cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllClusterResponse<ClusterInfoDetail>> getUserCluster(GetUserClusterRequest request) throws UnExpectedRequestException {
        return ControllerTemplate.metaDataQuery(LOGGER, "{&FAILED_TO_GET_CLUSTER}",
                () -> metaDataService.getUserCluster(request));
    }

    @POST
    @Path("db")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<DbInfoDetail>> getUserDbByCluster(GetUserDbByClusterRequest request) throws UnExpectedRequestException {
        return ControllerTemplate.metaDataQuery(LOGGER, "{&FAILED_TO_GET_DATABASE_BY_CLUSTER}",
                () -> metaDataService.getUserDbByCluster(request));
    }

    @POST
    @Path("table")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<TableInfoDetail>> getUserTableByDbId(GetUserTableByDbIdRequest request) throws UnExpectedRequestException {
        return ControllerTemplate.metaDataQuery(LOGGER, "{&FAILED_TO_GET_TABLE_BY_DATABASE}",
                () -> metaDataService.getUserTableByDbId(request));
    }

    @POST
    @Path("cs_table")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<CsTableInfoDetail>> getContextServiceTableByCsId(GetUserTableByCsIdRequest request) throws UnExpectedRequestException {
        return ControllerTemplate.metaDataQuery(LOGGER, "{&FAILED_TO_GET_TABLE_BY_CSID}",
                () -> metaDataService.getUserTableByCsId(request));
    }

    @POST
    @Path("column")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<ColumnInfoDetail>> getUserColumnByTableId(GetUserColumnByTableIdRequest request) throws UnExpectedRequestException {
        return ControllerTemplate.metaDataQuery(LOGGER, "{&FAILED_TO_GET_COLUMN_BY_TABLE}",
                () -> metaDataService.getUserColumnByTableId(request));
    }

    @POST
    @Path("cs_column")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<ColumnInfoDetail>> getUserColumnByContextService(GetUserColumnByCsRequest request) throws UnExpectedRequestException {
        return ControllerTemplate.metaDataQuery(LOGGER, "{&FAILED_TO_GET_COLUMN_BY_TABLE}",
                () -> metaDataService.getUserColumnByCsId(request));
    }

    @POST
    @Path("mul_db")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<MulDbResponse> addMultiDbRules(MulDbRequest request) throws UnExpectedRequestException {
        return ControllerTemplate.metaDataQuery(LOGGER, "{&FAILED_TO_ADD_MULTI_SOURCE_RULE}",
                () -> {
                    String dbs = metaDataService.addMultiDbRules(request);
                    return new GeneralResponse<>(ResponseStatusConstants.OK, "{&SUCCESS_MUL_DBS_COMPARE}", new MulDbResponse(dbs));
                });
    }

    @POST
    @Path("subSystemInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<SubSystemResponse>> getSubSystemInfo() throws UnExpectedRequestException {
        return ControllerTemplate.metaDataQuery(LOGGER, "{&FAILED_TO_GET_SUB_SYSTEM_INFO_CMDB}",
                () -> new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_SUB_SYSTEM_INFO_SUCCESS}", operateCiService.getAllSubSystemInfo()));
    }

    @POST
    @Path("productInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<ProductResponse>> getProductInfo() throws UnExpectedRequestException {
        return ControllerTemplate.metaDataQuery(LOGGER, "{&FAILED_TO_GET_PRODUCT_INFO_CMDB}",
                () -> new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_PRODUCT_INFO_SUCCESS}", operateCiService.getAllProductInfo()));
    }

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
    public GeneralResponse<List<CmdbDepartmentResponse>> getDepartmentInfoListByRoleType() throws UnExpectedRequestException {
        return ControllerTemplate.metaDataQuery(LOGGER, "{&FAILED_TO_GET_DEPARTMENT_INFO}",
                () -> new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_DEPARTMENT_INFO_SUCCESS}", metaDataService.getDepartmentInfoListByRoleType(null)));
    }

    @GET
    @Path("system/devAndOpsInfoWithRole/{deptCode}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<DepartmentSubResponse>> getDevAndOpsInfoByRoleTypeAndSourceType(@PathParam("deptCode") Integer deptCode) throws UnExpectedRequestException {
        return ControllerTemplate.metaDataQuery(LOGGER, "{&FAILED_TO_GET_DEPARTMENT_INFO}",
                () -> new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_DEPARTMENT_INFO_SUCCESS}", metaDataService.getDevAndOpsInfoListByRoleType(null, deptCode)));
    }

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

    @GET
    @Path("data_source/types/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> getAllDataSourceTypes(@QueryParam("proxyUser") String proxyUser) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return ControllerTemplate.dataSourceOperation(LOGGER, "Failed",
                () -> metaDataService.getAllDataSourceTypes(getClusterName(), proxyUser));
    }

    @GET
    @Path("data_source/env")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> getDataSourceEnv(@QueryParam("proxyUser") String proxyUser) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return ControllerTemplate.dataSourceOperation(LOGGER, "Failed",
                () -> metaDataService.getDataSourceEnv(getClusterName(), proxyUser));
    }

    @GET
    @Path("data_source/env/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<DataSourceEnvResponse>> envList(@QueryParam("dataSourceId") Long dataSourceId
                                                            , @QueryParam("dcnRangeType") String dcnRangeType) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return ControllerTemplate.dataSourceOperation(LOGGER, "Failed",
                () -> metaDataService.getEnvList(getClusterName(), dataSourceId, dcnRangeType));
    }

    @POST
    @Path("data_source/info/advance")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse getDataSourceAdvanceInfo(GetDataSourceRequest request) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return ControllerTemplate.dataSourceOperation(LOGGER, "Failed",
                () -> metaDataService.getDataSourceInfoWithAdvance(request));
    }

    @GET
    @Path("data_source/versions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> getDataSourceVersions(@QueryParam("proxyUser") String proxyUser
    , @QueryParam("dataSourceId") Long dataSourceId) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return ControllerTemplate.dataSourceOperation(LOGGER, "Failed",
                () -> metaDataService.getDataSourceVersions(getClusterName(), proxyUser, dataSourceId));
    }

    @GET
    @Path("data_source/info")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> getDataSourceInfoPage(@QueryParam("currentPage") Integer currentPage, @QueryParam("pageSize") Integer pageSize, @QueryParam("name") String searchName
            , @QueryParam("typeId") Long typeId, @QueryParam("typeName") String typeName) throws UnExpectedRequestException, PermissionDeniedRequestException {
        GetDataSourceRequest request = new GetDataSourceRequest();
        request.setName(searchName);
        request.setDataSourceTypeId(typeId);
        request.setDataSourceTypeName(typeName);
        request.setPage(currentPage);
        request.setSize(pageSize);
        return ControllerTemplate.dataSourceOperation(LOGGER, "Failed",
                () -> metaDataService.getDataSourceInfoWithAdvance(request));
    }

    @GET
    @Path("data_source/info/detail")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> getDataSourceInfoDetail(@QueryParam("proxyUser") String proxyUser
    , @QueryParam("dataSourceId") Long dataSourceId, @QueryParam("versionId") Long versionId) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return ControllerTemplate.dataSourceOperation(LOGGER, "Failed",
                () -> metaDataService.getDataSourceInfoDetail(getClusterName(), proxyUser, dataSourceId, versionId));
    }

    @GET
    @Path("data_source/key_define/type")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> getDataSourceKeyDefine(@QueryParam("proxyUser") String proxyUser, @QueryParam("keyId") Long keyId) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return ControllerTemplate.dataSourceOperation(LOGGER, "Failed",
                () -> metaDataService.getDataSourceKeyDefine(getClusterName(), proxyUser, keyId));
    }

    @POST
    @Path("data_source/connect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> connectDataSource(@QueryParam("proxyUser") String proxyUser, @RequestBody DataSourceConnectRequest request) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return ControllerTemplate.dataSourceOperation(LOGGER, "{&CONNECT_FAILED}",
                () -> metaDataService.connectDataSource(getClusterName(), proxyUser, request));
    }

    @POST
    @Path("data_source/publish")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> publishDataSource(@QueryParam("proxyUser") String proxyUser
        , @QueryParam("dataSourceId") Long dataSourceId, @QueryParam("versionId") Long versionId) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return ControllerTemplate.dataSourceOperation(LOGGER, "Failed",
                () -> metaDataService.publishDataSource(getClusterName(), proxyUser, dataSourceId, versionId));
    }

    @POST
    @Path("data_source/expire")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<Map<String, Object>> expireDataSource(@QueryParam("proxyUser") String proxyUser
        , @QueryParam("dataSourceId") Long dataSourceId) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return ControllerTemplate.dataSourceOperation(LOGGER, "Failed",
                () -> metaDataService.expireDataSource(getClusterName(), proxyUser, dataSourceId));
    }

    @POST
    @Path("data_source/modify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse modifyDataSource(@QueryParam("dataSourceId") Long dataSourceId, DataSourceModifyRequest request) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return ControllerTemplate.dataSourceOperation(LOGGER, "Failed",
                () -> metaDataService.modifyDataSource(getClusterName(), dataSourceId, request));
    }

    @POST
    @Path("data_source/param/modify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse modifyDataSourceParam(@QueryParam("dataSourceId") Long dataSourceId, @RequestBody DataSourceParamModifyRequest request) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return ControllerTemplate.dataSourceOperation(LOGGER, "Failed",
                () -> metaDataService.modifyDataSourceParam(getClusterName(), dataSourceId, request));
    }

    @POST
    @Path("data_source/create")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse createDataSource(@RequestBody DataSourceModifyRequest request) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return ControllerTemplate.dataSourceOperation(LOGGER, "Failed",
                () -> metaDataService.createDataSource(getClusterName(), request));
    }

    @GET
    @Path("data_source/dbs")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<DbInfoDetail>> getDbsByDataSource(@QueryParam("proxyUser") String proxyUser
        , @QueryParam("dataSourceId") Long dataSourceId, @QueryParam("envId") Long envId) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return ControllerTemplate.dataSourceOperation(LOGGER, "{&FAILED_TO_GET_DATABASE_BY_CLUSTER}",
                () -> metaDataService.getDbsByDataSource(getClusterName(), proxyUser, dataSourceId, envId));
    }

    @GET
    @Path("data_source/tables")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<TableInfoDetail>> getTablesByDataSource(@QueryParam("proxyUser") String proxyUser
        , @QueryParam("dataSourceId") Long dataSourceId, @QueryParam("envId") Long envId, @QueryParam("dbName") String dbName) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return ControllerTemplate.dataSourceOperation(LOGGER, "{&FAILED_TO_GET_TABLE_BY_DATABASE}",
                () -> metaDataService.getTablesByDataSource(getClusterName(), proxyUser, dataSourceId, dbName, envId));
    }

    @GET
    @Path("data_source/columns")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<ColumnInfoDetail>> getColumnsByDataSource(@QueryParam("proxyUser") String proxyUser
        , @QueryParam("dataSourceId") Long dataSourceId, @QueryParam("envId") Long envId, @QueryParam("dbName") String dbName, @QueryParam("tableName") String tableName) throws UnExpectedRequestException, PermissionDeniedRequestException {
        return ControllerTemplate.dataSourceOperation(LOGGER, "{&FAILED_TO_GET_COLUMN_BY_TABLE}",
                () -> metaDataService.getColumnsByDataSource(getClusterName(), proxyUser, dataSourceId, dbName, tableName, envId));
    }

    @POST
    @Path("udf/upload")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public GeneralResponse<String> uploadFile(@FormDataParam("file") InputStream fileInputStream, @FormDataParam("file") FormDataContentDisposition fileDisposition) {
        try {
            return fileService.uploadFile(fileInputStream, fileDisposition, "");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new GeneralResponse<>(ResponseStatusConstants.SERVER_ERROR, "{&FAILED_TO_UPLOAD_FILE}", null);
        }
    }

    @GET
    @Path("udf/directory")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<String>> getDirectory(@QueryParam("category") String category, @QueryParam("cluster_name") String clusterName) throws UnExpectedRequestException, PermissionDeniedRequestException, MetaDataAcquireFailedException {
        return ControllerTemplate.udfOperation(LOGGER, "{&FAILED_TO_GET_CATEGORY_LIST}",
                () -> metaDataService.getDirectory(category, clusterName));
    }

    @POST
    @Path("udf/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<UdfResponse> addUdf(UdfRequest udfRequest) throws UnExpectedRequestException, PermissionDeniedRequestException, MetaDataAcquireFailedException {
        return ControllerTemplate.udfOperation(LOGGER, "{&FAILED_TO_ADD_UDF}",
                () -> metaDataService.addUdf(udfRequest));
    }

    @POST
    @Path("udf/modify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<UdfResponse> modifyUdf(UdfRequest udfRequest) throws UnExpectedRequestException, PermissionDeniedRequestException, MetaDataAcquireFailedException {
        return ControllerTemplate.udfOperation(LOGGER, "{&FAILED_TO_MODIFY_UDF}",
                () -> metaDataService.modifyUdf(udfRequest));
    }

    @GET
    @Path("udf/detail")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<UdfResponse> getUdfDetail(@QueryParam("id") Long id) throws UnExpectedRequestException, PermissionDeniedRequestException, MetaDataAcquireFailedException {
        return ControllerTemplate.udfOperation(LOGGER, "{&FAILED_TO_GET_UDF}",
                () -> metaDataService.getUdfDetail(id));
    }

    @POST
    @Path("udf/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<DataInfo<UdfResponse>> getUdfAllWithPage(UdfRequest udfRequest) throws UnExpectedRequestException, PermissionDeniedRequestException, MetaDataAcquireFailedException {
        return ControllerTemplate.udfOperation(LOGGER, "{&FAILED_TO_GET_UDF}",
                () -> metaDataService.getUdfAllWithPage(udfRequest));
    }

    @POST
    @Path("udf/delete")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<UdfResponse> deleteUdf(UdfRequest udfRequest) throws UnExpectedRequestException, PermissionDeniedRequestException, MetaDataAcquireFailedException {
        return ControllerTemplate.udfOperation(LOGGER, "{&FAILED_TO_DELETE_UDF}",
                () -> metaDataService.deleteUdf(udfRequest));
    }

    @GET
    @Path("udf/switch")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<UdfResponse> switchUdfStatus(@QueryParam("id") Long id, @QueryParam("is_load") Boolean isLoad) throws UnExpectedRequestException, PermissionDeniedRequestException, MetaDataAcquireFailedException {
        return ControllerTemplate.udfOperation(LOGGER, "{&FAILED_TO_MODIFY_UDF}",
                () -> metaDataService.switchUdfStatus(id, isLoad));
    }

    @POST
    @Path("dcn")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse getDcn(GetDcnRequest getDcnRequest) throws UnExpectedRequestException {
        return ControllerTemplate.metaDataQuery(LOGGER, "Failed to get dcn tdsql info.",
                () -> {
                    CommonChecker.checkObject(getDcnRequest.getSubSystemId(), "sub_system_id");
                    CommonChecker.checkString(getDcnRequest.getDcnRangeType(), "dcn_range_type");
                    Object result = metaDataService.getDcnList(getDcnRequest.getSubSystemId(), getDcnRequest.getDcnRangeType(), Collections.emptyList());
                    return new GeneralResponse(ResponseStatusConstants.OK, "success", result);
                });
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
