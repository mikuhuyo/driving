package com.driving.driver.controller;

import cn.hutool.core.bean.BeanUtil;
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
import java.util.Map;

/**
 * @author YueLiMin
 * @version 1.0.0
 */
@RestController
@RequestMapping("/driver")
@Tag(name = "DriverController", description = "司机模块web接口")
public class DriverController {
    @Resource
    private IDriverService driverService;

    @PostMapping("/registerNewDriver")
    @Operation(summary = "司机注册")
    public R registerNewDriver(@RequestBody @Valid RegisterNewDriverForm registerNewDriverForm) {
        Map<String, Object> param = BeanUtil.beanToMap(registerNewDriverForm);
        String driverId = driverService.registerNewDriver(param);

        return R.ok().put("userId", driverId);
    }
}