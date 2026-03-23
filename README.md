# YNUFE 小工具（Android App）

> 一个用于同步/查看课表与成绩的 Android 应用（带账号管理、版本更新提示等）。

## 功能（Features）
- 启动时检查更新，并可跳转下载
- 用户登录（支持验证码）并同步：
  - 课表
  - 成绩
- 账号管理：保存多个账号、切换/删除
- 课程筛选：按“当前周”展示（基于学期开始日期）
- 成绩页面：支持搜索与及格/挂科筛选

## 技术栈（Tech Stack）
- Kotlin + Jetpack Compose
- Hilt（依赖注入）
- Retrofit + OkHttp（网络请求 + Cookie 管理）
- Room（本地持久化）
- Jsoup（HTML 解析）
- Coroutines + Flow（状态流驱动 UI）

## 架构说明（Architecture）
整体采用分层结构：
- `ui`：Compose 页面 + ViewModel（把 Flow 转成 UI State）
- `data`：
  - `api`：Retrofit 接口
  - `repository`：业务逻辑（网络/解析/入库协调）
  - `room`：Room Entity/Dao/Database
  - `model`：API 返回模型
- `di`：Hilt Module（Network / Repository / Database）


## 查看源码环境准备（Prerequisites）
- Android Studio（建议版本：最新版）
- JDK：17
- Android Gradle Plugin / Gradle：gradle-9.3.1-all
- 设备/模拟器：Android 7+