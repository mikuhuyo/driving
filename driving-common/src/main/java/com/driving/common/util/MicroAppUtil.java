package com.driving.common.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.driving.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * 微信小程序工具类
 * <p>获取授权</p>
 *
 * @author Administrator
 */
@Component("MicroAppUtil")
public class MicroAppUtil {

    @Value("${wx.app-id}")
    private String appId;

    @Value("${wx.app-secret}")
    private String appSecret;

    public String getOpenId(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session";
        HashMap<String, Object> map = new HashMap<>();
        map.put("appid", appId);
        map.put("secret", appSecret);
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String response = HttpUtil.post(url, map);
        JSONObject json = JSONUtil.parseObj(response);
        String openId = json.getStr("openid");
        if (openId == null || openId.length() == 0) {
            throw new RuntimeException("临时登陆凭证错误");
        }
        return openId;
    }

    public String getAccessToken() {
        String url = "https://api.weixin.qq.com/cgi-bin/token";
        HashMap<String, Object> map = new HashMap<>() {{
            put("grant_type", "client_credential");
            put("appid", appId);
            put("secret", appSecret);
        }};
        String response = HttpUtil.get(url, map);
        JSONObject json = JSONUtil.parseObj(response);
        if (json.containsKey("access_token")) {
            String accessToken = json.getStr("access_token");
            return accessToken;
        } else {
            throw new BusinessException(json.getStr("errmsg"));
        }
    }
}
