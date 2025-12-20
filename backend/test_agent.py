"""
测试智能体系统的脚本
用于验证MCP+RAG+LLM集成是否正常工作
"""

import os
import sys
from dotenv import load_dotenv

# 加载环境变量
load_dotenv()

# 添加当前目录到路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app import create_app

def test_agent():
    """测试智能体功能"""
    app = create_app()
    
    with app.app_context():
        from agent import DressCodeAgent
        from app import db, Post, User
        
        # 初始化智能体
        agent = DressCodeAgent(db, Post, User)
        
        # 测试查询
        test_queries = [
            "最近有什么穿搭推荐？",
            "今天天气怎么样？有什么穿搭建议？",
            "我要去约会，有什么推荐？",
            "上海最近有什么穿搭推荐？"
        ]
        
        print("=" * 50)
        print("智能体系统测试")
        print("=" * 50)
        
        for query in test_queries:
            print(f"\n用户查询: {query}")
            print("-" * 50)
            
            try:
                result = agent.process_query(
                    user_id=1,  # 假设用户ID为1
                    message=query,
                    history=[]
                )
                
                print(f"回复: {result.get('content', 'N/A')}")
                
                if result.get('weather'):
                    weather = result['weather']
                    print(f"天气: {weather.get('city', 'N/A')}, "
                          f"{weather.get('temperature', 'N/A')}°C, "
                          f"{weather.get('condition', 'N/A')}")
                
                if result.get('posts'):
                    print(f"推荐帖子数: {len(result['posts'])}")
                    for i, post in enumerate(result['posts'][:3], 1):
                        print(f"  {i}. 帖子ID: {post['id']}, "
                              f"标签: {post.get('tags', [])}")
                else:
                    print("推荐帖子数: 0")
                    
            except Exception as e:
                print(f"错误: {str(e)}")
                import traceback
                traceback.print_exc()
            
            print("-" * 50)
        
        print("\n测试完成！")

if __name__ == "__main__":
    # 检查环境变量
    if not os.environ.get("DASHSCOPE_API_KEY"):
        print("警告: 未设置 DASHSCOPE_API_KEY，LLM功能可能无法使用")
    
    if not os.environ.get("QWEATHER_API_KEY"):
        print("警告: 未设置 QWEATHER_API_KEY，天气功能将使用模拟数据")
    
    test_agent()


