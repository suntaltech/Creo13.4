# Creo 13.4 批量重命名插件交付包

[下载插件与图标完整包 r3（ZIP）](https://github.com/suntaltech/Creo13.4/raw/refs/heads/main/downloads/Creo13_Plugin_Icons_Complete.zip)

如果浏览器打开文件页面，请点击 **Download raw file / 下载原始文件**。
私有仓库需要先登录具有访问权限的 GitHub 账号。

包含修订后的 Java 源码、32×32 和 16×16 BMP 图标、PNG 源图、网页、安装/启动脚本、审核报告、补丁及测试记录。解压后先看 `README_先看这里.md`。

本次结果：21 项 Java 模拟/辅助检查、20 项网页模拟测试通过，包内文件哈希核验通过。
真实 Creo SDK 编译、Windows 安装/回退、Ribbon 图标显示及实机运行仍待验证；不含生产 class 或真实 RBN，不能认定实机卡死已解决。

SHA-256 校验值见 [downloads/SHA256SUMS.txt](downloads/SHA256SUMS.txt)。

Windows PowerShell 校验下载的 ZIP：

```powershell
Get-FileHash .\Creo13_Plugin_Icons_Complete.zip -Algorithm SHA256
```

交付日期：2026-10-28（北京时间）。
