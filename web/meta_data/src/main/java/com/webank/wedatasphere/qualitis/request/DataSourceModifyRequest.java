package com.webank.wedatasphere.qualitis.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.webank.wedatasphere.qualitis.constants.QualitisConstants;
import com.webank.wedatasphere.qualitis.exception.UnExpectedRequestException;
import com.webank.wedatasphere.qualitis.project.request.CommonChecker;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * @author allenzhou@webank.com
 * @date 2021/11/2 11:35
 */
public class DataSourceModifyRequest {
    private List<String> labels;
    private String createSystem;

    private String dataSourceDesc;
    private String dataSourceName;
    private Long dataSourceTypeId;
    private String subSystem;
    /**
     * 共享登录认证信息
     */
    private ConnectParams connectParams;
    /**
     * 录入方式（1-手动录入，2-自动录入）
     */
    private Integer inputType;
    /**
     * 认证方式（1-共享，2-非共享）
     */
    private Integer verifyType;
    private List<String> dcnSequence;
    /**
     * 环境配置
     */
    private List<DataSourceEnv> dataSourceEnvs;
    @JsonProperty("dcn_range_type")
    private String dcnRangeType;
    @JsonProperty("dev_department_name")
    private String devDepartmentName;
    @JsonProperty("ops_department_name")
    private String opsDepartmentName;
    @JsonProperty("dev_department_id")
    private Long devDepartmentId;
    @JsonProperty("ops_department_id")
    private Long opsDepartmentId;
    @JsonProperty("visibility_department_list")
    private List<DepartmentSubInfoRequest> visibilityDepartmentList;

    public String getDcnRangeType() {
        return dcnRangeType;
    }

    public void setDcnRangeType(String dcnRangeType) {
        this.dcnRangeType = dcnRangeType;
    }

    public List<String> getDcnSequence() {
        return dcnSequence;
    }

    public void setDcnSequence(List<String> dcnSequence) {
        this.dcnSequence = dcnSequence;
    }

    public Long getDevDepartmentId() {
        return devDepartmentId;
    }

    public void setDevDepartmentId(Long devDepartmentId) {
        this.devDepartmentId = devDepartmentId;
    }

    public Long getOpsDepartmentId() {
        return opsDepartmentId;
    }

    public void setOpsDepartmentId(Long opsDepartmentId) {
        this.opsDepartmentId = opsDepartmentId;
    }

    public String getDevDepartmentName() {
        return devDepartmentName;
    }

    public void setDevDepartmentName(String devDepartmentName) {
        this.devDepartmentName = devDepartmentName;
    }

    public String getOpsDepartmentName() {
        return opsDepartmentName;
    }

    public void setOpsDepartmentName(String opsDepartmentName) {
        this.opsDepartmentName = opsDepartmentName;
    }

    public List<DepartmentSubInfoRequest> getVisibilityDepartmentList() {
        return visibilityDepartmentList;
    }

    public void setVisibilityDepartmentList(List<DepartmentSubInfoRequest> visibilityDepartmentList) {
        this.visibilityDepartmentList = visibilityDepartmentList;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public String getSubSystem() {
        return subSystem;
    }

    public void setSubSystem(String subSystem) {
        this.subSystem = subSystem;
    }

    public String getCreateSystem() {
        return createSystem;
    }

    public void setCreateSystem(String createSystem) {
        this.createSystem = createSystem;
    }

    public String getDataSourceDesc() {
        return dataSourceDesc;
    }

    public void setDataSourceDesc(String dataSourceDesc) {
        this.dataSourceDesc = dataSourceDesc;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public Long getDataSourceTypeId() {
        return dataSourceTypeId;
    }

    public void setDataSourceTypeId(Long dataSourceTypeId) {
        this.dataSourceTypeId = dataSourceTypeId;
    }

    public ConnectParams getConnectParams() {
        return connectParams;
    }

    public void setConnectParams(ConnectParams connectParams) {
        this.connectParams = connectParams;
    }

    public Integer getVerifyType() {
        return verifyType;
    }

    public void setVerifyType(Integer verifyType) {
        this.verifyType = verifyType;
    }

    public Integer getInputType() {
        return inputType;
    }

    public void setInputType(Integer inputType) {
        this.inputType = inputType;
    }

    public List<DataSourceEnv> getDataSourceEnvs() {
        return dataSourceEnvs;
    }

    public void setDataSourceEnvs(List<DataSourceEnv> dataSourceEnvs) {
        this.dataSourceEnvs = dataSourceEnvs;
    }

    private static final String ENV_NAME_REGEX = "^[^,:]*$";

    public static void checkRequest(DataSourceModifyRequest request) throws UnExpectedRequestException {
        if (Integer.valueOf(QualitisConstants.DATASOURCE_MANAGER_INPUT_TYPE_AUTO).equals(request.getInputType())) {
            CommonChecker.checkListMinSize(request.getDcnSequence(), 1, "dcnSequence");
        }
        if (StringUtils.isNotBlank(request.getDcnRangeType())
                && !Arrays.asList("all", QualitisConstants.CMDB_KEY_DCN_NUM, QualitisConstants.CMDB_KEY_LOGIC_AREA).contains(request.getDcnRangeType())) {
            throw new UnExpectedRequestException("Invalid parameter: dcn_range_type");
        }
        List<DataSourceEnv> dataSourceEnvs = request.getDataSourceEnvs();
        CommonChecker.checkObject(dataSourceEnvs, "dataSourceEnvs");
        CommonChecker.checkListMinSize(dataSourceEnvs, 1, "dataSourceEnvs");
        Pattern pattern = Pattern.compile(ENV_NAME_REGEX);
        for (DataSourceEnv dataSourceEnv : dataSourceEnvs) {
            ConnectParams connectParams = dataSourceEnv.getConnectParams();
            CommonChecker.checkObject(connectParams, "dataSourceEnvs.connectParams");
            CommonChecker.checkString(connectParams.getHost(), "host");
            CommonChecker.checkString(connectParams.getPort(), "port");
            CommonChecker.checkString(dataSourceEnv.getEnvName(), "dataSourceEnvs.envName");
            Matcher matcher = pattern.matcher(dataSourceEnv.getEnvName());
            if (!matcher.matches()) {
                throw new UnExpectedRequestException("Invalid envName, cannot use ',' and ':'");
            }
            if (QualitisConstants.AUTH_TYPE_DPM.equals(connectParams.getAuthType())) {
                CommonChecker.checkString(connectParams.getMkPrivate(), "mkPrivate");
                CommonChecker.checkString(connectParams.getAppId(), "appId");
                CommonChecker.checkString(connectParams.getObjectId(), "objectId");
            } else if (QualitisConstants.AUTH_TYPE_ACCOUNT_PWD.equals(connectParams.getAuthType())) {
                CommonChecker.checkString(connectParams.getUsername(), "username");
                CommonChecker.checkString(connectParams.getPassword(), "password");
            }
        }
    }

    @Override
    public String toString() {
        return "DataSourceModifyRequest{" +
                "labels=" + labels +
                ", createSystem='" + createSystem + '\'' +
                ", dataSourceDesc='" + dataSourceDesc + '\'' +
                ", dataSourceName='" + dataSourceName + '\'' +
                ", dataSourceTypeId=" + dataSourceTypeId +
                ", subSystem='" + subSystem + '\'' +
                ", connectParams=" + connectParams +
                ", inputType=" + inputType +
                ", verifyType=" + verifyType +
                ", dcnSequence=" + dcnSequence +
                ", dataSourceEnvs=" + dataSourceEnvs +
                ", devDepartmentName='" + devDepartmentName + '\'' +
                ", opsDepartmentName='" + opsDepartmentName + '\'' +
                ", devDepartmentId=" + devDepartmentId +
                ", opsDepartmentId=" + opsDepartmentId +
                ", visibilityDepartmentList=" + visibilityDepartmentList +
                '}';
    }
}
