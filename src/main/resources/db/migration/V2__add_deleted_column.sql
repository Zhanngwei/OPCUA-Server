-- 添加deleted列到certificates表
ALTER TABLE certificates ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;

-- 更新现有记录，设置deleted为false
UPDATE certificates SET deleted = FALSE WHERE deleted IS NULL;

-- 确保deleted列不能为null
ALTER TABLE certificates ALTER COLUMN deleted SET NOT NULL;
