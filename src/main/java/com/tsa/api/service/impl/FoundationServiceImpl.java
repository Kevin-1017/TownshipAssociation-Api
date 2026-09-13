package com.tsa.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.DonationItemVO;
import com.tsa.api.dto.DonationRecordVO;
import com.tsa.api.dto.DonationSaveRequest;
import com.tsa.api.dto.FoundationHomeVO;
import com.tsa.api.dto.RewardCategorySaveRequest;
import com.tsa.api.dto.RewardItemVO;
import com.tsa.api.dto.RewardRecordSaveRequest;
import com.tsa.api.dto.RewardRecordVO;
import com.tsa.api.dto.RewardRecordsVO;
import com.tsa.api.entity.FoundationDonation;
import com.tsa.api.entity.FoundationRewardCategory;
import com.tsa.api.entity.FoundationRewardRecord;
import com.tsa.api.mapper.FoundationDonationMapper;
import com.tsa.api.mapper.FoundationRewardCategoryMapper;
import com.tsa.api.mapper.FoundationRewardRecordMapper;
import com.tsa.api.service.FoundationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 校友基金会业务实现。 */
@Service
@RequiredArgsConstructor
public class FoundationServiceImpl implements FoundationService {

    private final FoundationRewardCategoryMapper categoryMapper;
    private final FoundationRewardRecordMapper recordMapper;
    private final FoundationDonationMapper donationMapper;

    // ---------- 读 ----------

    @Override
    public FoundationHomeVO home() {
        FoundationHomeVO vo = new FoundationHomeVO();
        vo.setRewards(listCategories().stream().map(this::toRewardItem).toList());
        vo.setDonations(listDonationsDesc().stream().map(this::toDonationItem).toList());
        return vo;
    }

    @Override
    public RewardRecordsVO rewards() {
        Map<Long, String> nameById = listCategories().stream()
                .collect(Collectors.toMap(FoundationRewardCategory::getId, FoundationRewardCategory::getName));

        List<RewardRecordVO> records = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        for (FoundationRewardRecord r : listRecords()) {
            String categoryName = nameById.get(r.getCategoryId());
            RewardRecordVO vo = new RewardRecordVO();
            vo.setId(r.getId());
            vo.setCategoryId(r.getCategoryId());
            vo.setCategoryName(categoryName);
            vo.setRecipient(r.getRecipient());
            vo.setAmount(r.getAmount());
            records.add(vo);
            if (categoryName != null && !categories.contains(categoryName)) {
                categories.add(categoryName);
            }
        }
        RewardRecordsVO result = new RewardRecordsVO();
        result.setCategories(categories);
        result.setRecords(records);
        return result;
    }

    @Override
    public List<DonationRecordVO> donations() {
        // 显式映射：实体字段 donationDate 与 VO 字段 date 不同名，BeanUtil 拷不过去
        return listDonationsDesc().stream()
                .map(d -> {
                    DonationRecordVO vo = new DonationRecordVO();
                    vo.setId(d.getId());
                    vo.setDonorName(d.getDonorName());
                    vo.setAmount(visibleAmount(d));
                    vo.setDate(d.getDonationDate());
                    return vo;
                })
                .toList();
    }

    // ---------- 写：奖项类别 ----------

    @Override
    public Long createCategory(RewardCategorySaveRequest request) {
        FoundationRewardCategory entity = BeanUtil.copyProperties(request, FoundationRewardCategory.class);
        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        categoryMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateCategory(Long id, RewardCategorySaveRequest request) {
        FoundationRewardCategory existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "奖项类别不存在：id=" + id);
        }
        FoundationRewardCategory entity = BeanUtil.copyProperties(request, FoundationRewardCategory.class);
        entity.setId(id);
        categoryMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        FoundationRewardCategory existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "奖项类别不存在：id=" + id);
        }
        categoryMapper.deleteById(id);
        // 连带删除类别下的获奖记录，避免明细里出现「无归属」的孤儿记录
        recordMapper.delete(new LambdaQueryWrapper<FoundationRewardRecord>()
                .eq(FoundationRewardRecord::getCategoryId, id));
    }

    // ---------- 写：获奖记录 ----------

    @Override
    public Long createRecord(RewardRecordSaveRequest request) {
        requireCategory(request.getCategoryId());
        FoundationRewardRecord entity = BeanUtil.copyProperties(request, FoundationRewardRecord.class);
        recordMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateRecord(Long id, RewardRecordSaveRequest request) {
        FoundationRewardRecord existing = recordMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "获奖记录不存在：id=" + id);
        }
        requireCategory(request.getCategoryId());
        FoundationRewardRecord entity = BeanUtil.copyProperties(request, FoundationRewardRecord.class);
        entity.setId(id);
        recordMapper.updateById(entity);
    }

    @Override
    public void deleteRecord(Long id) {
        FoundationRewardRecord existing = recordMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "获奖记录不存在：id=" + id);
        }
        recordMapper.deleteById(id);
    }

    // ---------- 写：捐赠鸣谢 ----------

    @Override
    public Long createDonation(DonationSaveRequest request) {
        FoundationDonation entity = BeanUtil.copyProperties(request, FoundationDonation.class);
        donationMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateDonation(Long id, DonationSaveRequest request) {
        FoundationDonation existing = donationMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "捐赠记录不存在：id=" + id);
        }
        FoundationDonation entity = BeanUtil.copyProperties(request, FoundationDonation.class);
        entity.setId(id);
        donationMapper.updateById(entity);
    }

    @Override
    public void deleteDonation(Long id) {
        FoundationDonation existing = donationMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "捐赠记录不存在：id=" + id);
        }
        donationMapper.deleteById(id);
    }

    // ---------- private 辅助 ----------

    private List<FoundationRewardCategory> listCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<FoundationRewardCategory>()
                .orderByAsc(FoundationRewardCategory::getSort)
                .orderByAsc(FoundationRewardCategory::getId));
    }

    private List<FoundationRewardRecord> listRecords() {
        return recordMapper.selectList(new LambdaQueryWrapper<FoundationRewardRecord>()
                .orderByAsc(FoundationRewardRecord::getCategoryId)
                .orderByAsc(FoundationRewardRecord::getId));
    }

    private List<FoundationDonation> listDonationsDesc() {
        return donationMapper.selectList(new LambdaQueryWrapper<FoundationDonation>()
                .orderByDesc(FoundationDonation::getDonationDate));
    }

    private void requireCategory(Long categoryId) {
        FoundationRewardCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "所属奖项类别不存在：id=" + categoryId);
        }
    }

    private RewardItemVO toRewardItem(FoundationRewardCategory c) {
        RewardItemVO vo = new RewardItemVO();
        vo.setId(c.getId());
        vo.setLabel(c.getName());
        vo.setAmount(c.getAmount());
        vo.setSponsor(c.getSponsor());
        return vo;
    }

    private DonationItemVO toDonationItem(FoundationDonation d) {
        DonationItemVO vo = new DonationItemVO();
        vo.setId(d.getId());
        vo.setDonorName(d.getDonorName());
        vo.setAmount(visibleAmount(d));
        vo.setDate(d.getDonationDate());
        return vo;
    }

    /** 保密口径的唯一闸口：amount_visible 非 true 的记录一律不外发金额（库里金额照旧留存） */
    private Long visibleAmount(FoundationDonation d) {
        return Boolean.TRUE.equals(d.getAmountVisible()) ? d.getAmount() : null;
    }
}
