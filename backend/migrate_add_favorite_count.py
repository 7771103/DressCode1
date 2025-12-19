"""
数据库迁移脚本：添加 favorite_count 字段到 posts 表
运行此脚本以添加 favorite_count 字段并初始化现有数据
"""
import pymysql
import os

def migrate_database():
    """执行数据库迁移"""
    conn = None
    cursor = None
    try:
        # 连接数据库（使用与 app.py 相同的配置）
        user = os.environ.get("MYSQL_USER", "root")
        password = os.environ.get("MYSQL_PASSWORD", "123456")
        host = os.environ.get("MYSQL_HOST", "127.0.0.1")
        port = int(os.environ.get("MYSQL_PORT", "3306"))
        db_name = os.environ.get("MYSQL_DATABASE", "dresscode1")
        
        conn = pymysql.connect(
            host=host,
            port=port,
            user=user,
            password=password,
            database=db_name,
            charset='utf8mb4'
        )
        
        cursor = conn.cursor()
        
        print("开始数据库迁移...")
        
        # 1. 检查 favorite_count 字段是否已存在
        cursor.execute("""
            SELECT COUNT(*) 
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_SCHEMA = %s 
            AND TABLE_NAME = 'posts' 
            AND COLUMN_NAME = 'favorite_count'
        """, (db_name,))
        
        column_exists = cursor.fetchone()[0] > 0
        
        if not column_exists:
            print("添加 favorite_count 字段...")
            # 添加 favorite_count 字段
            cursor.execute("""
                ALTER TABLE posts 
                ADD COLUMN favorite_count INT DEFAULT 0 AFTER comment_count
            """)
            print("✓ favorite_count 字段已添加")
        else:
            print("✓ favorite_count 字段已存在，跳过添加")
        
        # 2. 初始化现有数据的 favorite_count
        print("初始化现有数据的 favorite_count...")
        cursor.execute("""
            UPDATE posts p
            SET p.favorite_count = (
                SELECT COUNT(*) 
                FROM favorites f 
                WHERE f.post_id = p.id
            )
        """)
        affected_rows = cursor.rowcount
        print(f"✓ 已更新 {affected_rows} 条帖子的收藏数量")
        
        # 提交更改
        conn.commit()
        print("\n数据库迁移完成！")
        
    except Exception as e:
        print(f"数据库错误: {e}")
        if conn:
            conn.rollback()
        import traceback
        traceback.print_exc()
    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()
            print("数据库连接已关闭")

if __name__ == "__main__":
    print("=" * 50)
    print("数据库迁移：添加 favorite_count 字段")
    print("=" * 50)
    print("\n请确保：")
    print("1. MySQL 服务正在运行")
    print("2. 数据库 'dresscode1' 已创建")
    print("\n使用环境变量或默认配置连接数据库...")
    print("(可通过环境变量 MYSQL_USER, MYSQL_PASSWORD, MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE 自定义)")
    print("\n开始迁移...\n")
    
    try:
        migrate_database()
    except KeyboardInterrupt:
        print("\n\n迁移已取消")
    except Exception as e:
        print(f"\n错误: {e}")
