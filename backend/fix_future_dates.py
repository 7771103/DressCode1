"""
修复数据库中所有未来日期的脚本
将所有未来日期改为当前时间
"""
import os
from datetime import datetime
import pymysql

# 数据库配置
DB_CONFIG = {
    "host": os.environ.get("MYSQL_HOST", "127.0.0.1"),
    "port": int(os.environ.get("MYSQL_PORT", "3306")),
    "user": os.environ.get("MYSQL_USER", "root"),
    "password": os.environ.get("MYSQL_PASSWORD", "123456"),
    "database": os.environ.get("MYSQL_DATABASE", "dresscode1"),
    "charset": "utf8mb4",
}


def fix_future_dates():
    """修复所有表中的未来日期"""
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    now = datetime.now()
    print(f"当前时间: {now}")
    print("=" * 60)
    
    # 需要检查的表和字段
    tables = [
        ("posts", "created_at"),
        ("likes", "created_at"),
        ("collections", "created_at"),
        ("comments", "created_at"),
        ("follows", "created_at"),
    ]
    
    total_fixed = 0
    
    for table_name, date_field in tables:
        print(f"\n检查表: {table_name}.{date_field}")
        
        # 查找未来日期的记录
        query = f"""
            SELECT id, {date_field} 
            FROM {table_name} 
            WHERE {date_field} > %s
            ORDER BY {date_field} DESC
        """
        cursor.execute(query, (now,))
        future_records = cursor.fetchall()
        
        if future_records:
            print(f"  找到 {len(future_records)} 条未来日期的记录:")
            for record_id, future_date in future_records[:10]:  # 只显示前10条
                print(f"    ID {record_id}: {future_date}")
            if len(future_records) > 10:
                print(f"    ... 还有 {len(future_records) - 10} 条记录")
            
            # 修复这些记录：将未来日期改为当前时间
            update_query = f"""
                UPDATE {table_name} 
                SET {date_field} = %s 
                WHERE {date_field} > %s
            """
            cursor.execute(update_query, (now, now))
            affected_rows = cursor.rowcount
            print(f"  ✓ 已修复 {affected_rows} 条记录")
            total_fixed += affected_rows
        else:
            print(f"  ✓ 没有发现未来日期的记录")
    
    conn.commit()
    cursor.close()
    conn.close()
    
    print("\n" + "=" * 60)
    print(f"修复完成！总共修复了 {total_fixed} 条记录")
    print("=" * 60)


if __name__ == "__main__":
    try:
        fix_future_dates()
    except Exception as e:
        print(f"错误: {e}")
        import traceback
        traceback.print_exc()

