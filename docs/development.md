# 开发流程与计划（对齐SOP）

文档版本: v1.0  
创建日期: 2025-09-03  
作者: 架构助手  
审核人: 待定

## 1. 开发流程概览
```mermaid
graph LR
  A[需求确认] --> B[技术方案]
  B --> C[任务拆分]
  C --> D[开发实现]
  D --> E[自测验证]
  E --> F[代码审查]
  F --> G[合并主干]
  G --> H[部署测试]
```

## 2. 环境与规范
- Android Studio 2021.3.1+；JDK 11/17；Gradle 8.1.1；SDK 24–34
- ktlint/detekt启用；.editorconfig统一缩进与行宽；中文注释强制
- 分支：main/develop/feature-*/bugfix-*/release/x.y.z

### 2.1 环境变量（AI）
- AI_API_BASE_URL、AI_API_KEY（必需）
- AI_API_CONNECT/READ/WRITE_TIMEOUT_MS、AI_API_RETRIES、AI_API_RETRY_BACKOFF_MS
- AI_API_PROVIDER_CHAIN（逗号分隔）
- 详见：docs/ai-config.md

## 3. 任务拆分（INVEST）
- 首页与导航（3人天）
- 主题与卡片列表（3人天）
- 播放器+互动模板（10人天）
- 奖励与贴纸（5人天）
- 下载模块（8人天）
- 家长门与时长（6人天）
- 推荐（Bandit+红色权重）（6人天）
- 家长报表+LLM摘要（6人天）
- 埋点/Crash/性能（3人天）
- QA测试与验收（8人天）

## 4. 技术方案要点
- Clean Architecture + MVVM；Room+DataStore；Retrofit+OkHttp
- AI端：Porcupine/Vosk KWS；TFLite/ONNX Mini-Intent；MediaPipe彩蛋（可选）
- AI云：OpenAI网关→ Gemini/GPT-5；Qwen3-Embed；BAAI Reranker；结果缓存
- 下载：WorkManager分块下载、断点续传、MD5校验、回滚

## 5. 自测清单
- 功能：用户故事逐项手测；下载/离线/奖励闭环
- 稳定：异常断网/存储不足/重启恢复
- 性能：启动≤2.5s；帧率、内存峰值；包体≤80MB
- 可用性：误触防抖；KWS命中；红色偏好开关

## 6. 代码审查（摘要）
- 自查：通过CI、注释齐全、更新文档
- 审查重点：功能正确、可维护、性能/安全、中文注释质量
- PR模板：概述/改动/测试/风险/截图/关联链接

## 7. 里程碑（4周）
- W1：方向冻结+事件方案+20卡内容流水线（TTS+审核）
- W2：骨架成型（首页/家长门/播放器v0/下载基础/KWS）
- W3：闭环可跑（奖励/时长/报表/推荐）内容至40卡
- W4：内容至60–72卡，性能打磨，弱网测试与内测发布

## 8. 风险与应对
- 低端机：模型量化、动效降级、资源LRU
- 内容产能：模板化与批处理，优先红色相关高热主题
- 合规：家长门+A/B易用性，法务审阅，敏感输出过滤

## 9. 交付清单
- 源码与注释、构建脚本、环境说明
- 文档：需求/架构/UX/测试/发布/埋点字典
- 构建产物：Release APK与符号；素材与校验记录

