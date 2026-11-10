# 离线审核步骤

1. 保存当前失败基线。输入 ZIP 的 SHA256、源文件散列和 DLL 报告散列见 evidence/source-register.json。该 DLL 散列来自现场记录，云端未复算，不覆盖原 DLL 或原始日志。
2. 核对补丁只针对输入包的 entry.cpp，基线 SHA256 为 b8ecc4df538cd246ef490ce0265d561c886d33d2bebea6f52d50b514bae5568e。可直接审核 src/entry.cpp，或在仅含基线 src/entry.cpp 的隔离副本目录中检查/应用零上下文补丁：

```sh
git apply --unidiff-zero --check /path/to/patches/entry-step35.diff
git apply --unidiff-zero /path/to/patches/entry-step35.diff
```

只在隔离副本应用；先校验基线散列，避免零上下文补丁误用到其他版本。云端已验证能应用并得到包内完整源码。

3. 离线核对本版 ProToolkit.h / ProToolkitDll.h，以及官方 user_initialize、user_terminate 文档的准确参数、方向、缓冲区大小、返回语义、调用约定和必须导出。若头文件不声明这些回调，仅“包含头文件能编译”不构成签名认证。不提供替代 SDK 头。
4. 比较已找到的 protk_install_example.zip 原始 makefile/vcxproj 条件：x64/Release、v143、/MT、宏、库、预处理和链接输入；若引用 .def 文件，读取其实际内容。核对是否有库成员强制引入、额外导出或初始化契约，先记录原文，不自行猜符号。只需针对性片段，不用全盘重扫或重新采集相同版本。
5. 如后续在具备真实 SDK 的隔离 Windows 构建环境审核编译，沿用已通过的 /c /GS /fp:precise /MT 与 _WSTDIO_DEFINED、PRO_MACHINE=36、PRO_OS=4，只把输入改为本包 entry.cpp，输出到新诊断目录。示例（不由本包自动执行）：

```bat
"%P1_CL%" /nologo /c /GS /fp:precise /MT /D_WSTDIO_DEFINED /DPRO_MACHINE=36 /DPRO_OS=4 /I"%P1_SDK_ROOT%\includes" /Fo"<新诊断目录>\entry_diag.obj" "src\entry.cpp"
```

目录先在隔离构建位置建立，变量指向真实同版工具链和 SDK。静态断言必须通过；出现头文件或接口错误时保留错误，不制造 Stub 或强制转换。当前云端未执行此命令。

6. 如需离线链接检查，沿用上轮已通过的三个 PTC 库、MT 运行库规则及系统库清单，改用新对象和独立诊断 DLL 输出名，保留原版本。检查 x64、两个导出、实际依赖及新散列；日志代码可能改变库抽取/依赖，不能强求仍只有 KERNEL32。不要使用 /FORCE、/NODEFAULTLIB、自定义 /ENTRY 或猜测导出绕过问题。本包不提供自动构建或加载脚本，也不输出已编译二进制。
7. 填写 review/diagnostic-result.template.json 的离线字段，保留未执行项为 null。当前交付范围到离线审核为止；诊断回调是否被宿主调用，只有后续专门安排的诊断观测才能确认，本包不要求现在部署或重试现场加载。

若后续另行开展诊断观测，临时 DAT 只在诊断副本中指向新的诊断 DLL 身份，保留原字段，不覆盖已注册或仍加载的文件，不同时进行目录、签名、CRT和初始化逻辑等多项试错。按 README 的证据分支解释结果，源码与 DLL 散列必须对应。
