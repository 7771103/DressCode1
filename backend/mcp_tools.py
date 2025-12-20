"""
MCP (Model Context Protocol) 工具模块
封装外部API调用，如天气API等
"""

import os
import requests
import logging
from typing import Dict, Optional

logger = logging.getLogger(__name__)


class WeatherTool:
    """天气工具 - MCP工具实现"""
    
    def __init__(self):
        self.api_key = os.environ.get("QWEATHER_API_KEY", "")
        self.base_url = "https://devapi.qweather.com/v7"
    
    def get_weather(
        self, 
        city: Optional[str] = None,
        location: Optional[str] = None
    ) -> Dict:
        """
        获取天气信息
        
        Args:
            city: 城市名称（如：北京、上海）
            location: 经纬度，格式：经度,纬度（如：116.41,39.92）
        
        Returns:
            天气信息字典，包含temperature、condition、forecast等
        """
        if not self.api_key:
            # 如果没有配置API Key，返回模拟数据
            logger.warning("未配置和风天气API Key，返回模拟数据")
            return {
                "city": city or "北京",
                "temperature": 25,
                "feelsLike": 27,
                "condition": "晴朗",
                "icon": "☀️",
                "humidity": 60,
                "windSpeed": "5km/h",
                "windDir": "东北风",
                "forecast": [
                    {
                        "date": "今天",
                        "tempMax": 28,
                        "tempMin": 20,
                        "textDay": "晴",
                        "textNight": "多云"
                    }
                ]
            }
        
        try:
            # 第一步：获取位置ID
            location_id = None
            if location:
                location_id = location
            else:
                search_url = f"{self.base_url}/city/lookup"
                search_params = {
                    "location": city or "北京",
                    "key": self.api_key,
                    "lang": "zh"
                }
                search_response = requests.get(search_url, params=search_params, timeout=10)
                search_data = search_response.json()
                
                if search_data.get("code") == "200" and search_data.get("location"):
                    location_id = search_data["location"][0]["id"]
                else:
                    logger.error(f"未找到城市：{city}")
                    return {
                        "city": city or "北京",
                        "error": f"未找到城市：{city}"
                    }
            
            # 第二步：获取实时天气
            weather_url = f"{self.base_url}/weather/now"
            weather_params = {
                "location": location_id,
                "key": self.api_key,
                "lang": "zh"
            }
            weather_response = requests.get(weather_url, params=weather_params, timeout=10)
            weather_data = weather_response.json()
            
            if weather_data.get("code") != "200":
                logger.error(f"获取天气失败：{weather_data.get('code', '未知错误')}")
                return {
                    "city": city or "北京",
                    "error": f"获取天气失败：{weather_data.get('code', '未知错误')}"
                }
            
            now = weather_data.get("now", {})
            
            # 第三步：获取3天天气预报
            forecast_url = f"{self.base_url}/weather/3d"
            forecast_params = {
                "location": location_id,
                "key": self.api_key,
                "lang": "zh"
            }
            forecast_response = requests.get(forecast_url, params=forecast_params, timeout=10)
            forecast_data = forecast_response.json()
            
            # 构建返回数据
            result = {
                "city": city or "北京",
                "locationId": location_id,
                "temperature": int(now.get("temp", 0)) if now.get("temp") else 0,
                "feelsLike": int(now.get("feelsLike", 0)) if now.get("feelsLike") else 0,
                "condition": now.get("text", "未知"),
                "icon": now.get("icon", ""),
                "humidity": now.get("humidity", "N/A"),
                "windSpeed": now.get("windSpeed", "N/A"),
                "windDir": now.get("windDir", "N/A"),
                "pressure": now.get("pressure", "N/A"),
                "vis": now.get("vis", "N/A"),
                "updateTime": now.get("obsTime", "")
            }
            
            # 添加3天预报
            if forecast_data.get("code") == "200" and forecast_data.get("daily"):
                result["forecast"] = []
                for day in forecast_data["daily"][:3]:
                    result["forecast"].append({
                        "date": day.get("fxDate", ""),
                        "tempMax": int(day.get("tempMax", 0)) if day.get("tempMax") else 0,
                        "tempMin": int(day.get("tempMin", 0)) if day.get("tempMin") else 0,
                        "textDay": day.get("textDay", ""),
                        "textNight": day.get("textNight", ""),
                        "iconDay": day.get("iconDay", "")
                    })
            
            logger.info(f"成功获取天气信息：{result['city']}, 温度：{result['temperature']}°C")
            return result
            
        except requests.exceptions.Timeout:
            logger.error("天气API请求超时")
            return {
                "city": city or "北京",
                "error": "请求超时，请稍后重试"
            }
        except requests.exceptions.RequestException as e:
            logger.error(f"天气API网络请求失败：{str(e)}")
            return {
                "city": city or "北京",
                "error": f"网络请求失败：{str(e)}"
            }
        except Exception as e:
            logger.error(f"获取天气信息失败：{str(e)}")
            return {
                "city": city or "北京",
                "error": f"获取天气信息失败：{str(e)}"
            }
    
    def get_user_city(self, user_id: int, User) -> Optional[str]:
        """
        获取用户所在城市
        
        Args:
            user_id: 用户ID
            User: User模型类
        
        Returns:
            城市名称，如果用户不存在或未设置城市则返回None
        """
        user = User.query.get(user_id)
        if user and user.city:
            return user.city
        return None


