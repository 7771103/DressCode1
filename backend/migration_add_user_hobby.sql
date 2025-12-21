-- Migration: Add hobby field to users table
-- Execute this SQL script in your MySQL database to add the hobby field
-- Date: 2024

USE `dresscode1`;

-- 添加 hobby 字段到 users 表
-- hobby 字段用于存储用户的个人爱好简介，类型为 TEXT，允许为空
ALTER TABLE `users` 
ADD COLUMN `hobby` TEXT DEFAULT NULL COMMENT '个人爱好简介' AFTER `gender`;

-- 验证字段是否添加成功
-- SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT 
-- FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_SCHEMA = 'dresscode1' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'hobby';

