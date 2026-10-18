# ych独家的一键root

Kotlin Android 项目源码：启动时检查 `su` 授权、显示机型/Android 版本/ABI/内核、导入 Magisk 模块 ZIP、检查 `module.prop`，经用户确认后调用 `magisk --install-module` 并显示日志。

## 推荐：只用安卓手机，在 GitHub 云端编译 APK

本项目包含 `.github/workflows/build-apk.yml`。它会在 GitHub 的 x86-64 Linux 云端运行 Gradle 8.9，避免在 ARM64 手机的 Termux/Ubuntu 环境中启动 x86-64 AAPT2 失败的问题。

1. 在浏览器打开 https://github.com/new ，登录或注册 GitHub，创建一个新仓库（例如 `ych-root-env-inspector`）。
2. 在手机文件管理器中解压这个 ZIP。
3. 打开新仓库页面，选择 **Add file → Upload files**，把解压后的项目文件和 `.github` 目录一起上传。确保 `app/`、`build.gradle.kts`、`settings.gradle.kts` 和 `.github/workflows/build-apk.yml` 位于仓库根目录；不要只上传 ZIP 本身。若手机页面无法显示隐藏的 `.github` 目录，可先上传其他文件，再在 GitHub 网页中依次创建 `.github/workflows/build-apk.yml` 并粘贴本文件的内容。
4. 提交（Commit changes）后，进入仓库的 **Actions** 页面，选择 **Build Android APK** 工作流，再按 **Run workflow**（如果 push 已自动触发，也可打开最新运行记录）。
5. 等待工作流变为绿色成功，打开该次运行，在 **Artifacts** 区域下载 `ych-dujia-root-debug-apk`，解压后得到 `app-debug.apk`，再在手机上安装。

如果构建失败，进入 Actions 的失败任务查看日志；失败日志可以发来继续排查。云端构建需要 GitHub 账号，并且仓库的 Actions 功能可用。

## 本机命令行构建

需要 JDK 17、Android SDK Platform 35、Build Tools 35.0.0，以及 Gradle 8.9：

```bash
gradle --no-daemon assembleDebug
```

## 注意

- 这是项目源码，不是已编译或实机验证的 APK；加入工作流也不代表构建已经成功。
- 需要 Magisk/root，且必须在授权弹窗中允许 `su`。
- ZIP 检查只做基本结构识别，不代表模块安全。仅安装可信来源文件，并提前备份。
- 硬编码的开发者密码 `ychnb666` 仅是演示用途，不是安全认证。
- 应用不能凭空给设备获取 root 权限；它只检查已有 root，并在已有授权时尝试安装 Magisk 模块。安装模块不保证实现 Root 隐藏，具体行为由模块本身决定。
