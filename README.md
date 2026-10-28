# Creo 13.4 批量重命名插件交付包

[下载插件与图标完整包 r4（16色BMP修正版）（ZIP）](https://github.com/suntaltech/Creo13.4/raw/refs/heads/main/downloads/Creo13_Plugin_Icons_Complete.zip)

如果浏览器打开文件页面，请点击 **Download raw file / 下载原始文件**。
私有仓库需要先登录具有访问权限的 GitHub 账号。

## r4 图标格式修正

用户反馈旧24位BMP导致 SetIcon 报 XToolkitBadInputs。本次改为4位索引色、16项Windows标准调色板、无压缩BMP；已检查文件头和调色板，修订后的Creo显示效果仍待实机确认。

- [单独下载32×32默认图标 batchrename.bmp](https://github.com/suntaltech/Creo13.4/raw/refs/heads/main/downloads/icons/batchrename.bmp)
- [单独下载16×16备用图标 batchrename_16.bmp](https://github.com/suntaltech/Creo13.4/raw/refs/heads/main/downloads/icons/batchrename_16.bmp)

已有插件功能正常时，无需重新编译或改 `.dat`：关闭Creo，备份并替换实际 `text_dir/resource/batchrename.bmp`，然后完整重启Creo以刷新图标缓存。默认路径为 `D:\creoson\jlink\text\resource\batchrename.bmp`。不要将16色索引BMP误作16位RGB BMP；16px备用文件不会被自动选择。

r4未改Java/网页逻辑；21项Java及20项网页模拟日志来自已验证的r3，并非本轮重新执行或实机验收。新增图标格式校验与旧格式拒绝测试通过，完整记录在包内。

包含修订后的 Java 源码、32×32 和 16×16 BMP 图标、PNG 源图、网页、安装/启动脚本、审核报告、补丁及测试记录。解压后先看 `README_先看这里.md`。

本次结果：21 项 Java 模拟/辅助检查、20 项网页模拟测试通过，包内文件哈希核验通过。
真实 Creo SDK 编译、Windows 安装/回退、Ribbon 图标显示及实机运行仍待验证；不含生产 class 或真实 RBN，不能认定实机卡死已解决。

SHA-256 校验值见 [downloads/SHA256SUMS.txt](downloads/SHA256SUMS.txt)。

Windows PowerShell 校验下载的 ZIP：

```powershell
Get-FileHash .\Creo13_Plugin_Icons_Complete.zip -Algorithm SHA256
```

交付日期：2026-10-28（北京时间）。
