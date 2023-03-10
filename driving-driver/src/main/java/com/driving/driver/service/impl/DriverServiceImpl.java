package com.driving.driver.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codingapi.txlcn.tc.annotation.LcnTransaction;
import com.driving.common.exception.BusinessException;
import com.driving.common.util.MicroAppUtil;
import com.driving.common.util.PageUtils;
import com.driving.driver.controller.form.UpdateDriverAuthForm;
import com.driving.driver.entity.Driver;
import com.driving.driver.entity.DriverSettings;
import com.driving.driver.entity.Wallet;
import com.driving.driver.mapper.DriverMapper;
import com.driving.driver.mapper.DriverSettingsMapper;
import com.driving.driver.mapper.WalletMapper;
import com.driving.driver.service.IDriverService;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.iai.v20200303.IaiClient;
import com.tencentcloudapi.iai.v20200303.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author YueLiMin
 * @version 1.0.0
 */
@Slf4j
@Service
public class DriverServiceImpl implements IDriverService {
    @Value("${tencent.cloud.secretId}")
    private String secretId;
    @Value("${tencent.cloud.secretKey}")
    private String secretKey;
    @Value("${tencent.ai-face.groupName}")
    private String groupName;
    @Value("${tencent.ai-face.groupId}")
    private String groupId;
    @Value("${tencent.ai-face.region}")
    private String region;

    @Resource
    private MicroAppUtil microAppUtil;
    @Resource
    private DriverMapper driverMapper;
    @Resource
    private DriverSettingsMapper driverSettingsMapper;
    @Resource
    private WalletMapper walletMapper;

    @Override
    @LcnTransaction
    @Transactional(rollbackFor = {Exception.class})
    public void updateDriverRealAuth(Long driverId, Integer realAuth) {
        Driver driver = new Driver();
        driver.setRealAuth(realAuth);

        driverMapper.update(driver, new LambdaQueryWrapper<Driver>().eq(Driver::getId, driverId));

        // todo ??????????????????????????????????????????
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    @LcnTransaction
    public String registerNewDriver(Map<String, Object> map) {
        // ?????????????????????
        String code = MapUtil.getStr(map, "code");
        // ?????????????????????
        String openId = microAppUtil.getOpenId(code);

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("openId", openId);
        // ????????????????????????
        if (driverMapper.hasDriver(hashMap) != 0) {
            throw new BusinessException("???????????????");
        }

        // ????????????
        map.put("openId", openId);
        driverMapper.registerNewDriver(map);

        // ????????????id
        String driverId = driverMapper.searchDriverIdByOpenId(openId);

        DriverSettings driverSettings = new DriverSettings();
        driverSettings.setDriverId(Long.parseLong(driverId));
        // ????????????-????????????
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("orientation", "");
        jsonObject.put("listenService", true);
        jsonObject.put("orderDistance", 0);
        jsonObject.put("rangeDistance", 5);
        jsonObject.put("autoAccept", false);
        driverSettings.setSettings(jsonObject.toJSONString());
        // ??????????????????
        driverSettingsMapper.insert(driverSettings);

        // ??????????????????
        Wallet wallet = new Wallet();
        wallet.setDriverId(Long.parseLong(driverId));
        wallet.setBalance(new BigDecimal("0"));
        // ????????????
        wallet.setPassword("666666");
        // ????????????????????????
        walletMapper.insert(wallet);

        return driverId;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    @LcnTransaction
    public void updateDriverAuth(UpdateDriverAuthForm form) {
        Driver driver = new Driver();
        BeanUtil.copyProperties(form, driver);
        driver.setId(form.getDriverId());

        // 1????????? 2????????? 3?????????
        driver.setRealAuth(3);
        driverMapper.updateById(driver);
    }

    @Override
    @LcnTransaction
    @Transactional(rollbackFor = {Exception.class})
    public String createDriverFaceModel(Long driverId, String base64) {
        log.info("driver id -> {}, base64 -> {}, groupId -> {}", driverId, base64, groupId);

        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            return "????????????????????????[??????????????????]";
        }

        String result = "success";

        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("iai.tencentcloudapi.com");

        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        Credential cred = new Credential(secretId, secretKey);
        IaiClient client = new IaiClient(cred, region, clientProfile);

        try {
            Long gender = "???".equals(driver.getSex()) ? 1L : 2L;

            CreatePersonRequest req = new CreatePersonRequest();
            req.setGroupId(groupId);
            req.setPersonId(driverId + "");
            req.setPersonName(driver.getName());
            req.setGender(gender);
            req.setQualityControl(4L);
            req.setUniquePersonControl(4L);
            req.setImage(base64);
            CreatePersonResponse resp = client.CreatePerson(req);

            if (StrUtil.isNotBlank(resp.getFaceId())) {
                Driver temp = new Driver();
                temp.setId(driverId);
                temp.setArchive(true);

                driverMapper.updateById(temp);
            }
        } catch (TencentCloudSDKException sdkException) {
            log.error(sdkException.getMessage());
            result = sdkException.getMessage();
        } catch (Exception e) {
            log.error("??????????????????????????????", e);
            result = "??????????????????????????????";
        }

        return result;
    }

    @Override
    public Boolean driverFaceAuth(Long driverId, String base64) {
        boolean isAuth = false;

        Boolean driverFaceRecognition = this.driverFaceRecognition(driverId, base64);
        Boolean driverInVivoDetection = this.driverInVivoDetection(driverId, base64);

        if (driverFaceRecognition && driverInVivoDetection) {
            isAuth = true;
        }

        return isAuth;
    }

    @Override
    public HashMap<String, Object> driverLogin(String openid, String phone) {
        // ????????????
        String openId = microAppUtil.getOpenId(openid);
        Driver driver = driverMapper.selectOne(new LambdaQueryWrapper<Driver>().eq(Driver::getOpenId, openId).ne(Driver::getStatus, 2));
        if (driver == null) {
            throw new BusinessException("???????????????");
        }

        // ???????????????-????????????????????????????????????????????????
        // String tel = microAppUtil.getTel(phone);
        // if (!tel.equals(driver.getTel())) {
        //     throw new BusinessException("??????????????????????????????????????????");
        // }

        // ????????????????????????????????? 0????????? 1?????????
        Integer archive = driver.getArchive() ? 1 : 0;
        // ???????????? 1????????? 2????????? 3?????????
        Integer realAuth = driver.getRealAuth();

        HashMap<String, Object> map = new HashMap<>();
        map.put("archive", archive);
        map.put("realAuth", realAuth);
        map.put("driverId", driver.getId());

        return map;
    }

    @Override
    public HashMap<String, Object> searchDriverBaseInfo(Long driverId) {
        HashMap<String, Object> map = driverMapper.searchDriverBaseInfo(driverId);
        // summary ??????????????????json??????
        cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(MapUtil.getStr(map, "summary"));
        map.replace("summary", jsonObject);
        return map;
    }

    @Override
    public HashMap<String, Object> searchDriverAuthInformation(Long driverId) {
        return driverMapper.searchDriverAuthInformation(driverId);
    }

    @Override
    public PageUtils searchDriverByPage(HashMap<String, Object> hashMap) {
        ArrayList<HashMap<String, Object>> result = null;

        Long driverCount = driverMapper.searchDriverCount(hashMap);
        if (driverCount == null) {
            driverCount = 0L;
            result = new ArrayList<>();
        } else {
            result = driverMapper.searchDriverByPage(hashMap);
        }

        int start = (Integer) hashMap.get("start");
        int end = (Integer) hashMap.get("length");

        return new PageUtils(result, driverCount, start, end);
    }

    @Override
    public HashMap<String, Object> searchDriverRealSummary(Long driverId) {
        return driverMapper.searchDriverRealSummary(driverId);
    }

    /**
     * ??????????????????
     *
     * @param driverId ??????id
     * @param base64   ????????????
     * @return Boolean
     */
    private Boolean driverFaceRecognition(Long driverId, String base64) {
        Boolean flag = false;

        Credential cred = new Credential(secretId, secretKey);

        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("iai.tencentcloudapi.com");

        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        IaiClient client = new IaiClient(cred, region, clientProfile);

        VerifyFaceRequest req = new VerifyFaceRequest();
        req.setPersonId(driverId + "");
        req.setQualityControl(4L);
        req.setImage(base64);

        try {
            VerifyFaceResponse resp = client.VerifyFace(req);
            flag = resp.getIsMatch();
        } catch (TencentCloudSDKException exception) {
            log.error(exception.getMessage());
        } catch (Exception e) {
            log.error("??????????????????????", e);
        }

        return flag;
    }

    /**
     * ??????????????????
     *
     * @param driverId ??????id
     * @param base64   ????????????
     * @return Boolean
     */
    private Boolean driverInVivoDetection(Long driverId, String base64) {
        boolean flag = false;

        Credential cred = new Credential(secretId, secretKey);

        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("iai.tencentcloudapi.com");

        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        IaiClient client = new IaiClient(cred, region, clientProfile);

        DetectLiveFaceAccurateRequest req = new DetectLiveFaceAccurateRequest();
        req.setImage(base64);

        try {
            DetectLiveFaceAccurateResponse resp = client.DetectLiveFaceAccurate(req);
            flag = resp.getScore() >= 40;
        } catch (TencentCloudSDKException exception) {
            log.error(exception.getMessage());
        } catch (Exception e) {
            log.error("??????????????????????", e);
        }

        return flag;
    }

}
