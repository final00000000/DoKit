# DoKit Android 17 适配版

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-API%2037-brightgreen.svg)](#一适配基线)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-purple.svg)](#一适配基线)
[![AGP](https://img.shields.io/badge/AGP-9.2.1-orange.svg)](#一适配基线)

> 本仓库基于 [didi/DoKit](https://github.com/didi/DoKit)（Apache-2.0），完成了 **Android 17（API 37）** 的工具链升级、Gradle 插件迁移和运行时行为适配，并保留原有 DoKit 调试能力。
>
> 上游完整项目介绍与使用手册请访问：[didi/DoKit](https://github.com/didi/DoKit) · [DoKit 官网](https://www.dokit.cn/)

---

## 一、适配基线

| 项目 | 版本 |
|---|---|
| Compile SDK | **37（Android 17）** |
| Target SDK | **37** |
| Min SDK | 21（部分模块 16） |
| Gradle | **9.4.1** |
| Android Gradle Plugin | **9.2.1** |
| Kotlin / KGP | **2.4.21** |
| 构建 JDK | **17**（buildSrc `jvmTarget = 17`） |
| ASM | 9.9 |

---

## 二、适配内容

### 2.1 工具链升级

- Gradle `7.0.2 → 9.4.1`，AGP `4.2.2 → 9.2.1`，Kotlin `1.3/1.4 → 2.4.21`。
- `compileSdkVersion` / `targetSdkVersion` 统一提升到 **37**。
- 在 `buildSrc/build.gradle` 中显式钉住 KGP 2.4.21，避免 AGP 传递依赖的旧 KGP 覆盖构建脚本类路径。
- `buildSrc` 与 Gradle 插件源码的 `jvmTarget` 提升到 17；业务模块仍保持 Java 8 字节码兼容。

### 2.2 Gradle 插件迁移（AGP Artifact API）

旧版 DoKit 插件依赖 Booster 和已废弃的 Transform API，在 AGP 9 下无法使用。现已重写为：

- 使用 `ScopedArtifact.CLASSES` + `toTransform` 注册转换。
- 新增 `DoKitTransformTask`，基于 ASM 9.9 处理 class jar / 目录。
- 移除 Booster 依赖以及 `DoKit*Transform`、`DoKitTransformInvocation`、`ProxyHandlerCallback` 等旧实现。
- 保留原有 12 个 Transformer（网络、GPS、大图、GSM、入口慢函数等），顺序与行为保持一致。
- 仅对 debug 变体插桩，release 变体继续使用 `dokit-no-op`。

### 2.3 Android 17 运行时行为适配

| 适配点 | 说明 |
|---|---|
| **本地网络权限** | 新增 `LocalNetworkPermission`，在 DoKit Connect、文件互传、MC 主机/客户端入口请求 `ACCESS_LOCAL_NETWORK`；低版本自动跳过。 |
| **通知权限** | 声明 `POST_NOTIFICATIONS`；demo 在 API 33+ 主动请求。 |
| **前台服务类型** | 录屏/取色使用 `FOREGROUND_SERVICE_MEDIA_PROJECTION`，按 `mediaProjection` 类型调用 `startForeground`。 |
| **MediaProjection** | Android 14+ 先启动前台服务再 `getMediaProjection`，并注册 `MediaProjection.Callback` 后再创建 VirtualDisplay。 |
| **Edge-to-edge** | `BaseActivity` / `NewBaseActivity` 统一按系统栏 + 刘海 insets 设置内容 padding；demo 的 `BaseStatusBarActivity` 同步处理。 |
| **预测性返回** | WebView 等页面改用 `OnBackPressedDispatcher`，移除旧 `onBackPressed` 重写。 |
| **PendingIntent** | 全部补 `FLAG_IMMUTABLE`，避免 API 31+ 崩溃。 |
| **卡顿检测** | API 37+ 改用公开的 `Choreographer` 帧回调；移除对 `ActivityThread.mH` / `Instrumentation` 隐藏 API 的 hook。 |
| **MessageQueue** | 不再反射 `MessageQueue` 私有字段，兼容 Android 17 的无锁实现。 |
| **动态广播** | `registerReceiver` 在 API 33+ 显式传入 `RECEIVER_EXPORTED`。 |
| **网络监听** | `CONNECTIVITY_ACTION` 迁移到 `ConnectivityManager.NetworkCallback`，旧系统保留广播兜底。 |
| **Activity 耗时** | 使用 API 29+ 的 pre/post 生命周期回调，旧系统使用公开回调近似，不再依赖隐藏 API。 |

### 2.4 依赖迁移（去 JCenter）

| 原依赖 | 现方案 |
|---|---|
| `com.android.volley:volley:1.1.1` | `1.2.1`（Google Maven） |
| `com.google.android.flexbox` 旧版 | `3.0.0`（Google Maven） |
| `com.tencent.tbs:tbssdk:43903` | `44286`（Maven Central） |
| `com.blankj:utilcodex:1.29.0` | `1.31.1` |
| `pub.devrel:easypermissions:2.0.1` | `3.0.0` |
| `com.github.ybq:SpinKit` | 系统 `ProgressBar` |
| `com.lzy.okgo:okgo` | OkHttp（demo 已改写） |
| `com.squareup.leakcanary:leakcanary-android` 旧版 | `2.14` |
| `com.facebook.fresco:fresco-processors` | 移除（未使用） |

Weex、BRVAH、Epic 等仅存在于 JCenter 的包，在标准仓库之后增加阿里云 `public` 作为**最后兜底**；Google Maven / Maven Central 仍优先。

### 2.5 构建与变体调整

- 所有模块补齐 `namespace`，满足 AGP 9 的 namespace 唯一性要求。
- debug-only 的 demo 代码与布局（MC、X5、录屏、悬浮窗、百度地图页面等）移入 `src/debug`，release 变体可正常构建。
- `dokit-gps-mock` 已通过 `api` 暴露百度 jar，app 侧排除重复 `BaiduLBS_Android.jar`，解决 AGP 9 `mergeJavaRes` 重复 asset 冲突。
- 兼容 AGP 9 的非传递 R 类，暂时保留 `android.nonTransitiveRClass=false`（后续应收敛到正确的 R 引用）。
- 恢复被历史提交误删的百度 / 高德 / 腾讯 demo 地图 key，修复百度 SDK 初始化 NPE。

---

## 三、构建指南

### 3.1 环境要求

- JDK 17
- Android SDK Platform **37**、Build-Tools **37.0.0**
- 建议使用 Android Studio 最新稳定版

### 3.2 常用命令

```bash
cd Android

# 调试包
./gradlew assembleDebug

# 发布包
./gradlew assembleRelease

# Lint
./gradlew :app:lintDebug

# 发布插件
./gradlew :dokit-plugin:assemble
```

### 3.3 产物

- `Android/app/build/outputs/apk/debug/app-debug.apk`
- `Android/app/build/outputs/apk/release/app-release.apk`
- 各模块 AAR（13 个）
- `Android/dokit-plugin/build/libs/dokit-plugin.jar`

### 3.4 网络受限环境

若访问 `dl.google.com` / Maven Central 出现 TLS 握手问题，可传入镜像参数：

```bash
./gradlew assembleDebug \
  -PANDROID_REPOSITORY_MIRROR=https://maven.aliyun.com/repository/google \
  -PPUBLIC_REPOSITORY_MIRROR=https://maven.aliyun.com/repository/public
```

> 在此类网络下不要使用 `--refresh-dependencies`，全量重新校验会长时间卡在 TLS 握手上。

---

## 四、验证结果

| 检查项 | 结果 |
|---|---|
| `assembleDebug` | ✅ BUILD SUCCESSFUL |
| `assembleRelease` | ✅ BUILD SUCCESSFUL |
| `:app:lintDebug` | ✅ **0 errors**（260 warnings） |
| `:dokit-plugin:assemble` | ✅ BUILD SUCCESSFUL |
| DoKit 插桩任务 | ✅ 在 app debug 变体正常执行 |
| 真机冷启动 | ✅ API 34 设备验证通过（无 FATAL EXCEPTION） |

---

## 五、已知限制与后续工作

- **Android 18（API 38）尚未发布**：Google 官方暂无 SDK / 文档，当前最高为 API 37.2 Canary，暂无可适配内容。
- **API 37 真机运行时验证待补**：本地验证设备为 Android 14（API 34），本地网络权限、edge-to-edge、大屏方向限制尚未在 API 37 实机/模拟器上验证。
- **新版一机多控界面待补权限检查**：`MultiControlAllFragment` 的 host/client 入口尚未接入 `LocalNetworkPermission`。
- **release demo 启动页**继承的 `old/BaseActivity` 尚未处理 edge-to-edge insets。
- **大屏方向限制**：DoKit 部分 Activity 仍声明 `screenOrientation="portrait"`，target 37 在 sw≥600dp 设备上会被系统忽略，需大屏专项测试。
- **16KB page size**：demo 依赖中的 `epic`、`fresco 1.13`、`wcdb` 原生库未按 16KB 对齐，16KB 设备上可能加载失败。
- **ASM frames**：`DoKitTransformTask` 目前使用 `COMPUTE_MAXS`；release 的 `minifyEnabled` 仍为 false，R8 路径尚未验证，建议改为可靠的 `COMPUTE_FRAMES`。
- **AGP 9 过渡开关**：`android.builtInKotlin=false`、`android.newDsl=false`、`android.nonTransitiveRClass=false` 为临时兼容项，后续应迁移 Kotlin DSL 并移除。
- **单元测试基线**：`WebSocketClientTest` 中的 `Thread.currentThread().join()` 会导致测试永久挂起，`dokit-mc` 的示例测试依赖未 mock 的 `org.json`，测试套件尚未全绿。
- **发布前检查**：`config.gradle` 的 `archives_type` / `run_env` / `use_local` 当前为本地开发配置；版本号仍为 `3.7.14.9-kotlin-13`，正式发布前需恢复配置并升级版本。

---

## 六、上游与许可

- 上游项目：[didi/DoKit](https://github.com/didi/DoKit)
- 上游文档：[DoKit 官网](https://www.dokit.cn/) · [上游 README](https://github.com/didi/DoKit/blob/master/README.md)
- 本项目遵循 **Apache License 2.0**，详见 [LICENSE](LICENSE)；原始代码版权归滴滴及 DoKit 贡献者所有。

> DoKit 的所有功能仅建议在 Debug 环境使用，Release 环境请自行充分测试。
