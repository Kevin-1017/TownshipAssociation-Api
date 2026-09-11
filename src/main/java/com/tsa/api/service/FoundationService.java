package com.tsa.api.service;

import com.tsa.api.dto.DonationRecordVO;
import com.tsa.api.dto.DonationSaveRequest;
import com.tsa.api.dto.FoundationHomeVO;
import com.tsa.api.dto.RewardCategorySaveRequest;
import com.tsa.api.dto.RewardRecordSaveRequest;
import com.tsa.api.dto.RewardRecordsVO;

import java.util.List;

/**
 * 校友基金会业务接口：读（首页聚合 + 明细）与管理端写（CRUD）。
 *
 * <p>写接口一期不鉴权（管理后台二期上线），故放在非 admin 路径下。
 */
public interface FoundationService {

    // ---------- 读 ----------

    /** 首页聚合：奖励类别 + 捐赠鸣谢。 */
    FoundationHomeVO home();

    /** 奖励明细：类别名列表 + 获奖记录（含类别名）。 */
    RewardRecordsVO rewards();

    /** 捐赠明细：按日期倒序。 */
    List<DonationRecordVO> donations();

    // ---------- 写：奖项类别 ----------

    Long createCategory(RewardCategorySaveRequest request);

    void updateCategory(Long id, RewardCategorySaveRequest request);

    /** 删除类别，并连带删除其下的获奖记录。 */
    void deleteCategory(Long id);

    // ---------- 写：获奖记录 ----------

    Long createRecord(RewardRecordSaveRequest request);

    void updateRecord(Long id, RewardRecordSaveRequest request);

    void deleteRecord(Long id);

    // ---------- 写：捐赠鸣谢 ----------

    Long createDonation(DonationSaveRequest request);

    void updateDonation(Long id, DonationSaveRequest request);

    void deleteDonation(Long id);
}
