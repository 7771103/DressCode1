-- 数据库迁移脚本：添加 favorite_count 字段到 posts 表
-- 运行此 SQL 脚本以添加 favorite_count 字段并初始化现有数据

-- 1. 添加 favorite_count 字段（如果不存在）
ALTER TABLE posts 
ADD COLUMN IF NOT EXISTS favorite_count INT DEFAULT 0 AFTER comment_count;

-- 2. 初始化现有数据的 favorite_count
UPDATE posts p
SET p.favorite_count = (
    SELECT COUNT(*) 
    FROM favorites f 
    WHERE f.post_id = p.id
);

