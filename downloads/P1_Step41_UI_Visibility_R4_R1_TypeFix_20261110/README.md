# Step41 UI R4-R1：修正不透明命令 ID 的日志类型

日期：2026-11-10（北京时间）。本包替代不能编译的 R4 诊断源码；只修正诊断 ID 的类型契约，不声称修复 UI 不可见问题。

## 1. 改了什么文件

仅 src/entry.cpp 的 ID 格式部分：新增 cstdint/cinttypes，按本机真实 typedef void *uiCmdCmdId 检查类型，使用 reinterpret_cast<std::uintptr_t>(command_id) 及 PRIxPTR 输出。command_id 从数字字段改为带 0x 前缀的十六进制字符串；新事件 revision=UI_R4_R1_DIAG，事件名仍为 CMD_IDENTITY_BEFORE_DESIGNATE。

```cpp
static_assert(std::is_same<uiCmdCmdId, void*>::value,
    "Verify the supplied Creo 13.4 opaque command ID typedef");
// 格式片段：
"\"command_id\":\"0x%" PRIxPTR "\",\"sdk_action_name\":\"%s\","
// 对应参数：
reinterpret_cast<std::uintptr_t>(command_id)
```

不把指针当成可解引用的对象，不解引用、不计算偏移、不释放。不把它转换为 int/long，不用 %lld 输出指针；不将其作为 JSON 数字，以免后续汇总工具损失 64 位精度。它仅作为同一进程/同一次加载中的不透明身份，不能跨运行比较，也不能把某个数值或 0x0 直接解释成 SDK 有效/无效判据。

基线是上次交付的完整 R4 源码：SHA256 af8b3f34ddea53151bff23c899c8ddf7ff197da81c34f57fc7b30eb3c84960e1。此次上传只有编译日志和接口片段，没有本机源码；先核对基线哈希再替换完整源码或应用 patches/opaque-command-id.diff。补丁为零上下文：git apply --unidiff-zero --check，通过后用 git apply --unidiff-zero 应用。其他基线不要强行套用。

## 2. 为什么改

本机真实 ProUICmd.h 声明 uiCmdCmdId 为 void*。R4 错误地假设它为整数，同时使用 integral static_assert 和 static_cast<long long>，导致 C2338/C2440；相关 C4477/C4473 格式警告也必须在重编时重新核查。此错误来自诊断候选，不是 Creo 注册/运行失败，不要求调整 SDK、编译器、CRT、入口或 DAT。

现有 ProCmdActionAdd/ProCmdDesignate 声明与原调用形状一致：Add 通过 uiCmdCmdId* 写回，Designate 接受该 ID。现在保留这一传递过程，只正确格式化旁路日志。接口完整语义注释、指定元数据的可见性要求仍未提供；不能由签名断言 UI 根因。

此次本机报告的三个资源 SHA256 都与此前收到的资源字节一致：p1_fc.txt、p1_readonly_model_info.txt、p1_readonly_ping.txt。故不要求再次采集同一批文件字节，也不改编码、键、说明或资源内容。哈希一致本身仍不能证明 Creo 实际解析了哪一路径或语言目录；若后续需要定位查找关系，只补这部分缺失证据。

## 3. 预期通过时看到什么

本机助手先按原固定工具链离线重编、链接，核对源码/DLL 哈希及既有导出/依赖要求。不复用旧对象；不需要重复环境调查。C2338/C2440 和本次 ID 格式关联 C4477/C4473 应消失；若仍有这些警告，停止并回传准确诊断。SDK 头文件的 C4819 与本次 ID 类型错误分开记录，不为它改写厂商头文件。

云端已运行 checks/formatting.cpp：它使用从候选直接摘取的完整 snprintf 表达式，独立验证标准 C++ 格式化、JSON 可解析性、重复指针保持同一字符串、不同本地对象地址可区分、完整 uintptr_t 位宽输出。使用 g++ -std=c++11 -Wall -Wextra -Wformat=2 -Werror 编译；没有 SDK 头文件替身或模拟 TOOLKIT 调用。这不是 Windows/MSVC/Creo SDK 编译通过证据。

如需按既有流程进行一次受控观察，由用户手动加载隔离候选即可；不部署正式版本。原 R4 的 Body → ModelInfo → Ping 顺序和六项调用保持不变。每个 Add 成功后、Designate 前各有一条身份事件，共三条。全部属于同一 pid/load_token；command_id 必须是十六进制字符串，其余键映射应与 R3/R4 一致。

只在同一初始化段判断三个 ID 是否不同；不要固定期望具体地址，也不要把不同 ID 等同于 UI 验收。填 acceptance.template.json。沿用相同模型/模式/过滤条件观察列表；仍不可见则无需绑定或点击，不加做面计数验收。

## 4. 失败时如何分支

- 基线哈希不符：停止套补丁，回传实际源码身份；不改动不明来源源码。
- 类型检查仍失败：回传实际 typedef 和编译诊断，不删除检查、不强制降为整数、不伪造 SDK 声明。
- 编译/链接失败或格式警告仍在：标记离线失败；不进入现场注册，也不归类为 UI 失败。
- 身份事件缺失：先核对候选 DLL、初始化路径、同一 load_token 和原有日志 I/O 信号。
- ID 重复：保留原值及对应动作键，进一步结合 SDK 契约审核；不自动重新注册、覆盖或执行这些 ID。
- 三个 ID 不同、六项返回 0但仍不可见：身份日志检查通过，UI 仍失败；接着审核真实 Designate 注释/官方本机帮助及资源解析关系，不再随机改名。
- 新候选可见：记录实际观察；本轮只是类型修正，不能据此认定已经找到稳定 UI 修复或计数功能通过。

## 5. 不做什么

不改 SDK 动作键 p1_readonly_fc、显示标签、逻辑功能标识、消息键/文件、资源字节、注册顺序、优先级、标志、access、三个业务回调或面计数逻辑。不新增 SDK API，不改日志文件位置和原有业务事件；只修正 ID 表达与诊断版本标识。

不打开、选择、修改、保存、转换模型，不自动启动/停止 Creo，不改 DAT/config.pro、缓存、CRT 或 plus 链接要求。云端未进行真实 SDK 构建、DLL 加载或 UI 测试。本包不包含 DLL、SDK、活动注册文件、原始编译日志或本机绝对路径。
