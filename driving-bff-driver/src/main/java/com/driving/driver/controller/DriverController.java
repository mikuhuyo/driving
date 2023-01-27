package com.driving.driver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.driving.common.util.R;
import com.driving.driver.controller.form.RegisterNewDriverForm;
import com.driving.driver.service.IDriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * @author YueLiMin
 * @version 1.0.0
 */
@RestController
@RequestMapping("/driver")
@Tag(name = "DriverController", description = "司机bff模块web接口")
public class DriverController {
    @Resource
    private IDriverService driverService;

    @PostMapping("/registerNewDriver")
    @Operation(summary = "司机bff端司机注册")
    public R registerNewDriver(@RequestBody @Valid RegisterNewDriverForm registerNewDriverForm) {
        String userId = driverService.registerNewDriver(registerNewDriverForm);
        // sa-token登录
        StpUtil.login(userId);
        // sa-token获取token value
        String tokenValue = StpUtil.getTokenInfo().getTokenValue();

        return R.ok().put("token", tokenValue);
    }
}
