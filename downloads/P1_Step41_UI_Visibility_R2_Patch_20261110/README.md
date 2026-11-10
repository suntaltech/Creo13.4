# Step41 UI R2：仅交换首末注册顺序

日期：2026-11-10（北京时间）。R1 短消息文件名试验未解决问题，本轮不再改资源名。唯一变量是 user_initialize 中三个注册函数的调用顺序：面计数与 Ping 交换，Model Info 保持第二。

## 1. 改了什么文件

仅 src/entry.cpp 中的两行调用位置发生变化，patches/ui-registration-order.diff 是同一修改的最小补丁。基线为本次交接包 entry_r1.cpp，SHA256：13d74fa1eec8e5059c205a225af771f4110069aff18eff9398373e4c1e097cac。

```cpp
// R1：Ping → Model Info → Body Face Count
// R2：Body Face Count → Model Info → Ping
register_readonly_body_face_count();
register_readonly_model_info();
register_readonly_ping();
```

以上注释仅用于说明，不新增到实际源码。实际交付仅做调用顺序交换，不改任一注册函数或回调函数的定义。可用完整源码覆盖隔离副本，或先校验基线，再运行 git apply --unidiff-zero --check 检查并应用同一 diff。

## 2. 为什么改

本包原件核对结果：

- 提交的 entry_r1.cpp 与 R1 交付逐字节一致，p1_fc.txt 也逐字节一致。
- 日志有两次初始化。只看最后一次初始化所属的 pid/load_token，六个 ActionAdd/Designate 状态均为 0，初始化返回前为 0；截图仍只有 Model Info 和 Ping，没有面计数项。R1 单独缩短消息名未奏效。
- 前一段日志出现 TERM_ENTER/TERM_BEFORE_RETURN，可确认前一应用走到这两个标记；不能当作新 R1 的终止结果，也不能证明 OS 已移除模块。

从当前源码可以核对的注册/指定条件：三个命令使用相同优先级、标志、参数顺序和同一个始终返回 ACCESS_AVAILABLE 的访问回调；都在 user_initialize 内、成功返回前调用。指定调用均报告成功，但 UI 列表实际可见性仍是独立观察。

本包没有本版 ProCmdDesignate 完整文档，所以不能据现象宣称“每 DLL 最多两个命令”、补造 UI 刷新 API，或要求一个未经确认的图标/菜单步骤。现有证据同时具有“前两项可见、第三项缺失”的位置关联，值得先作受控交换，而不是继续随机缩短描述或改名。

Model Info 保持第二，作为不变对照。面计数的访问回调只是引用已定义的 readonly_ping_access 函数，它不依赖 Ping 命令已注册，所以可以先注册面计数。统计函数从未在初始化中调用；移动注册位置不执行面遍历。

## 3. 预期通过时看到什么

本机助手沿用 R1 的构建、库、plus 强制引用、DAT、短名资源及其实际位置，重编新对象并记录新 DLL 身份。不要重写或重新布置任何消息文件；本包不附资源，继续使用同一份 p1_fc.txt 和旧消息文件。不要改变模型、当前模式、命令名、标签、键名、描述、优先级或访问标志。

R2 生效的首要证据是新启动段里的日志顺序：

1. INIT_ENTER。
2. BODY_FACE_COUNT_REGISTER_BEGIN → BODY_FACE_COUNT_ACTION_ADD_RETURN=0 → BODY_FACE_COUNT_DESIGNATE_RETURN=0。
3. MODEL_INFO_REGISTER_BEGIN → MODEL_INFO_ACTION_ADD_RETURN=0 → MODEL_INFO_DESIGNATE_RETURN=0。
4. CMD_REGISTER_BEGIN → CMD_ACTION_ADD_RETURN=0 → CMD_DESIGNATE_RETURN=0（Ping）。
5. INIT_BEFORE_RETURN，return_code=0。

不要把文件中旧 load_token 的事件混入本轮。若顺序仍为 Ping 先，则 R2 没有被有效加载，不能给出 UI 因果结论。

完成原有候选替换/加载流程后，在相同条件下查看 TOOLKIT 命令列表一次，记录三个名称各自是否可见。列表按名称排序，不能用 UI 行号判断注册顺序；注册顺序以日志为准。不要为了试验清除 UI 设置、换模型或反复重启。

若面计数项出现，再按原机制绑定并只触发一次，记录 BODY_FACE_COUNT_ENTER/RETURN；UI 可见性与计数回调结果分开填写。无模型时返回 no_active_model 可以证明触发路径，不能当作默认体面遍历已通过。

## 4. 预期失败时如何分支

| R2 结果（六个状态均为 0 且新日志顺序正确） | 能说明什么 |
| --- | --- |
| 面计数、Model Info 可见；末位 Ping 消失 | 强化“缺失跟随末位/登记批次或 UI 提交时序”的假设，削弱原面计数身份独有问题的解释；不等于证明 SDK 硬限制为两个命令。下一步按该证据及真实 SDK 文档核对，不添加虚假占位命令。 |
| 面计数仍缺失，另外两个仍可见 | 单纯首末顺序改变不足以解决，继续聚焦该命令身份/指定资源或 UI 元数据；不改面计数算法，不再将“第三个注册”当成已证根因。 |
| 三个都可见 | 该重排在本次条件下有效；保留结果，不能仅凭一次结果宣称官方要求特定注册顺序或已解释内部缓存。 |
| 出现其他缺失组合 | 记录各命令身份及当前段状态，保留未决，不从列表位置猜原因。 |

如编译、链接、plus 导出或任一注册/指定状态未通过，或出现同名旧条目冲突，先记录本变量未被有效测试并停止；不能把这些结果与原来“六项都为 0 但缺一项”的条件混合。若面计数项可见但触发失败，保留 UI 已通过与回调未通过两个状态，不在本轮修计数逻辑。

## 5. 不做什么

不改 p1_fc.txt 或任何其他资源，不改文件名、命令名、显示文字、消息键、优先级、标志和 access 函数；不新增 API、图标、菜单、刷新调用或新的日志字段。原计数回调、模型信息、Ping、日志实现及入口签名/返回值逐字保留；不改 CRT、链接要求、DAT 或 config.pro。

不打开、选择、修改、保存模型，不读新增几何参数，不做转换，不自动启动/停止 Creo，不删除缓存或重置用户界面。本包只给一个调用顺序变量补丁，真实 SDK 编译和一次最小 GUI 验证由本机助手执行；云端只有补丁应用与字节检查，没有宣称 UI 已修复。
