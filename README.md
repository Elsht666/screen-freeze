# 贴膜防误触 · Screen Freeze

手机贴膜时的防误触助手。贴膜过程中一键锁住屏幕触摸，屏幕保持常亮白屏，方便对齐、贴合钢化膜，贴完再解锁。

## 特性

- **贴膜模式**：一键锁定屏幕触摸，覆盖层切换为纯白全屏，贴膜时能清晰看到屏幕边界
- **屏幕常亮**：锁定后屏幕保持常亮、无状态栏，贴膜过程中不会黑屏
- **音量键退出**：同时按住「音量上 + 音量下」2 秒，退出贴膜模式
- **悬浮锁大小可调**：A- / A / A+ 三档，随时缩小、恢复正常、放大悬浮锁
- **完全离线**：应用完全离线本地运行，无联网功能，不存储任何数据
- **中文界面**：全中文汉化，界面极简，只保留必要操作

## 下载

[下载 APK](https://raw.githubusercontent.com/Elsht666/screen-freeze/main/release/ScreenFreeze.apk)

> 版本：1.3.6-internal · 安装包约 8 MB

## 使用步骤

1. 安装 APK 后打开应用
2. 点击「授予权限」，在系统无障碍设置中开启本应用
3. 返回应用，点击「开启」锁定屏幕触摸（进入贴膜模式）
4. 贴膜完成后，同时按住音量上 + 音量下 2 秒退出
5. 右上角「关于」可查看仓库与开发者信息

## 从源码构建

需要 Android SDK。项目使用 Gradle Wrapper，构建命令：

```bash
# Windows
gradlew.bat :app:assembleInternalDebug --offline

# macOS / Linux
./gradlew :app:assembleInternalDebug --offline
```

APK 输出路径：`app/build/outputs/apk/internal/debug/`

## 项目结构

- `app/` — 主应用模块（Kotlin，无障碍服务 + 悬浮锁）
- `featureunlocker/` — 功能解锁辅助模块
- `release/` — 预编译 APK

## 致谢

本项目基于开源项目 **Touch Blocker** 改造（Apache License 2.0，Copyright 2024 Eric Cochran），在原版基础上汉化界面并针对"贴膜防误触"场景做了定制。

## License

    Copyright 2024 Eric Cochran（原版 Touch Blocker）
    Modified by Elsht

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
