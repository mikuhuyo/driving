package com.driving.mis.service.impl;

import com.driving.common.util.PageUtils;
import com.driving.mis.dao.DeptDao;
import com.driving.mis.pojo.DeptEntity;
import com.driving.mis.service.DeptService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class DeptServiceImpl implements DeptService {
    @Resource
    private DeptDao deptDao;

    @Override
    public ArrayList<HashMap> searchAllDept() {
        ArrayList<HashMap> list = deptDao.searchAllDept();
        return list;
    }

    @Override
    public HashMap searchById(int id) {
        HashMap map = deptDao.searchById(id);
        return map;
    }

    @Override
    public PageUtils searchDeptByPage(Map param) {
        ArrayList<HashMap> list = deptDao.searchDeptByPage(param);
        long count = deptDao.searchDeptCount(param);
        int start = (Integer) param.get("start");
        int length = (Integer) param.get("length");
        PageUtils pageUtils = new PageUtils(list, count, start, length);

        return pageUtils;
    }


    @Override
    public int insert(DeptEntity dept) {
        int rows = deptDao.insert(dept);
        return rows;
    }

    @Override
    public int update(DeptEntity dept) {
        int rows = deptDao.update(dept);
        return rows;
    }

    @Override
    public int deleteDeptByIds(Integer[] ids) {
        if (!deptDao.searchCanDelete(ids)) {
            throw new HxdsException("无法删除关联用户的部门");
        }
        int rows = deptDao.deleteDeptByIds(ids);
        return rows;
    }
}
