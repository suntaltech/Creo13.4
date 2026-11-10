# Step39：p1_readonly_ping 单一最小补丁

日期：2026-11-10（北京时间）。承接 Step38 启动契约突破，按《P1 与 GPT 高效协作 SOP》交付。本轮唯一功能变化：注册并提供一个可由 GUI 绑定触发的 p1_readonly_ping，触发后只写 JSONL，不访问任何模型。

## 1. 改了什么文件

| 文件 | 内容 |
| --- | --- |
| src/entry.cpp | 在既有初始化入口注册一个命令；回调只记录 READONLY_PING。通过真实 SDK typedef 检查回调类型，不使用函数指针强转。 |
| text/p1_readonly_ping.txt | ProCmdDesignate 所需的三个最小消息条目，让同一个命令可用于 GUI 绑定。没有菜单、图标或页面。 |
| build/required-link-options.rsp | 固定已验证的两个 plus 强制引用；这是已通过构建契约的记录，不是新的实验变量。 |
| patches/readonly-ping.diff | 一个补丁，针对已交付的 Step35 诊断源码，包含上述源码变化及两个新支持文件。 |

基线 entry.cpp SHA256：00f74550913b12b4bc6bff5827459a18329f7b732f75b64dd0db9877a736dea5。可在隔离副本直接采用包内完整源码；如使用 diff，先校验基线，再运行 git apply --unidiff-zero --check，检查后仅在副本应用。

日志实现、文件名、tag、入口名称/签名和成功返回值保持原样。因此日志仍为 GetTempPathW 目录中的 p1-step35-<PID>.jsonl，tag 仍是 P1_STEP35_DIAG_V1；这不是用错版本。新增事件 facts.command 明确标识 p1_readonly_ping，并继续使用 seq、load_token 和 UTC 关联本次操作。

## 2. 为什么改

按 Step38 现场实测摘要，官方样例和补入 plus 引导层的诊断 DLL 均成功启动，诊断记录了 INIT_ENTER 与成功返回前标记。此前最小链接模板遗漏 plus 包装入口强制引用，是这次已定位并已由现场修正的构建问题；本轮不再猜测修改签名、返回值、DAT、CRT 或授权条件。

早先实际 exports.log 有 user_initialize 和 user_terminate 两个普通入口，缺少 plus 入口；不是普通入口名称需要改写。后续链接必须同时保留：

```text
/INCLUDE:user_initialize_plus
/INCLUDE:user_terminate_plus
```

不要把普通入口手动重命名为 plus，不做导出别名，不新增 DllMain。PTC 静态库提供包装层。只检查两个普通导出已不足以验收这一 DLL 启动契约。

本轮用 ProCmdActionAdd 注册唯一命令，ProCmdDesignate 配合最小消息资源使其可在 GUI 中绑定。只注册而未指定，不能保证用户能在自定义 TOOLKIT 命令列表中找到它。没有增加模型 API、菜单逻辑或自动执行动作。

## 3. 预期通过时看到什么

本机助手沿用既定工具链离线构建；用户只在助手准备好后做一次命令触发。先完成这些构建检查，不重复采集既定版本：

- 使用已验证的 SDK Include/Lib 显式补充方式，不能只调用 vcvarsall 就忽略 SDK 路径补充。
- 保留 /MT、既定编译宏和选项；链接使用 protk_dll_NU.lib、ucore.lib、udata.lib、libcmt.lib 及已验证系统库。
- 保留两个 /INCLUDE；可沿用原有显式选项，或在链接中引用 @build/required-link-options.rsp，无需重复添加。不要再拿 Step33 的旧链接命令覆盖已修正的构建。
- 源码改变后必须重编新对象，并记录新 DLL 散列；不要只链接旧 entry.obj。dumpbin 应包含两个 plus 入口。当前源码仍声明两个普通导出，它们不应被重命名；官方样例仅导出 plus 的布局不能拿来反推普通入口必须重命名。

消息资源由本机助手按本机已成功官方样例的消息目录解析方式，放入当前应用 text_dir 对应的正确位置。不修改 DAT 字段或 config.pro，不改既有 SDK 安装。包内 text/p1_readonly_ping.txt 是要部署到该资源布局的文件，不附带一个新的 DAT。若资源解析失败，以 CMD_DESIGNATE_RETURN 为依据反馈，不同时更换多个目录/字段试错。消息目录为四行一条的记录格式，文件尾空行属于格式，不能自动清理；补丁应用时可用 --whitespace=nowarn 保留该格式。

正常初始化日志顺序包含：

1. INIT_ENTER。
2. CMD_REGISTER_BEGIN，command=p1_readonly_ping。
3. CMD_ACTION_ADD_RETURN，pro_error=0。
4. CMD_DESIGNATE_RETURN，pro_error=0。
5. INIT_BEFORE_RETURN，return_code=0。

之后用户从已准备的 GUI 入口只点一次，新增一条 READONLY_PING，facts 包含 command=p1_readonly_ping、result=ok、model_accessed=false。日志沿用 UTC Z 后缀；北京时间加 8 小时。

ProCmdDesignate 不会自动在当前 Ribbon 创建按钮。本机助手先确认命令可见并引导一次必要绑定，或使用已有的命令触发机制；不让用户写代码、试命令或随机点击。若找不到此命令，停在绑定检查，不用旧命令代替本轮测试。一次点击只应产生一条本命令的 READONLY_PING；访问状态回调不记日志，避免 GUI 刷新产生伪触发。

## 4. 预期失败时如何分支

| 现象 | 本机助手处理 |
| --- | --- |
| 真实 SDK 编译失败 | 保留首个完整错误及构建结果回传；不自行改核心 C++、声明假原型或强制转换回调。云端未持有真实头文件，没有宣称已编译通过。 |
| plus 导出缺失或重新启动失败 | 先核对是否遗漏已固定链接选项、是否复用了旧对象/DLL；不再回头猜签名或改返回值。 |
| CMD_ACTION_ADD_RETURN 非 0 | 命令注册未通过，后续不尝试指定；保持错误码，停止本次命令验收。不要重用、覆盖或触发同名旧命令。 |
| 注册成功但 CMD_DESIGNATE_RETURN 非 0 | 检查本补丁消息资源、实际资源解析布局及错误码；不将 DLL 正在运行等同于命令可见。 |
| 两项为 0，但没有 READONLY_PING | 先核对新 DLL 身份、实际点击命令和绑定；复用已知日志通道，查必要调试输出，不能直接认定回调没有执行。 |
| 点击产生多条 Ping | 记录实际点击次数、seq/load_token 及绑定，结束本次观察，不重复点击补结果。 |

成功返回值仍保持 PRO_TK_NO_ERROR，不因注册/指定失败而改成猜测的宿主返回值。因此 DLL 启动成功不代表命令注册成功，必须分别检查上述 SDK 返回记录和真正点击日志。本轮没有自动重试或同名命令查找/覆盖逻辑。

终止日志沿用既有 TERM_ENTER/TERM_BEFORE_RETURN；此前 Step38 仅报告初始化成功，不能预填终止/卸载通过。停止后不再点该按钮；若出现同名旧命令或停止异常，如实记录，不自行添加未知的删除 API 或反复启停。

## 5. 不做什么

不读取当前模型、不打开或选择模型/特征、不查询几何、不创建特征、不写模型、不保存、不转换。不新增 DllMain、不改入口签名、返回值、DAT、CRT、config.pro，不自动启动 Creo，不写 Ribbon 布局文件，不生成正式部署版。

本包只有一个功能补丁、消息资源和固定链接选项，不再附加采集工具。本机助手负责哈希、实际离线编译链接、dumpbin、必要现场引导和结果回传；用户只做必要 GUI 操作。云端完成补丁应用和静态范围检查，没有真实 SDK 编译或现场运行验证。下一轮只反馈这一个命令的结果，不跳到转换。
