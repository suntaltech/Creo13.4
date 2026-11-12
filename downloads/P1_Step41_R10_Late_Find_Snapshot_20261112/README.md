# Step41 R10：Ping触发一次晚期Find快照

日期：2026-11-12（北京时间）。这是离线源码审核候选，不是正式插件发布，不含DLL、资源或配置，不实现模型转换。

## 1. 改了什么

基于指定R9 Access V2 entry.cpp：SHA256 19329fb972f3dbb9f1bf8a44790b5bcb9f68ff9215384d9465d607e4eb8eb839。新增静态初始化基线记录、晚期快照及有界日志格式化函数；在原Ping的READONLY_PING事件之后调用一次性快照入口。

原R9每命令初始化Find仍执行一次。其结果发布到对应基线，保存动作名、工程日志标识、ActionAdd/Find ID、Find原始返回码。只在Find成功且两个ID均非空时保存有效指针对；否则两个基线指针均为null，保留原Find码。原R9事件仍保留原始ActionAdd ID，便于区分“没有有效可比较指针对”与“ActionAdd没发生”。未走到初始化Find时，晚期日志baseline_initialized=false、基线返回码为null，不捏造成功码。

基线使用0空/1写入中/2已发布的原子状态，每个命令在同一次DLL加载中只发布首次初始化观测，之后不可变。晚期只读已发布记录，不等待、不解引用指针。会话定义为同一pid/load_token所标识的DLL加载周期，不跨DLL重载比较，不在终止时重置门闩或进行Find。

首次Ping用一次InterlockedCompareExchange取得快照执行权，固定Body → ModelInfo → Ping各Find一次。每条BEGIN/RETURN包含revision=UI_R10_LATE_FIND、phase=on_demand_ping_action、probe_ordinal=1、command_ordinal=1/2/3及查询前probe_tick。额外字段包含初始化基线有效性、原返回码和两组ID比较。opaque ID仅格式化或比较，不执行对应命令。

第二次及以后Ping仍保留READONLY_PING，然后只新增LATE_ID_FIND_PROBE_ALREADY_DONE，不再Find。如果首次仍在执行或未正常执行完，此事件snapshot_complete=false，不能冒称完整快照已经完成；门闩仍不重试。三个查询均返回后state=2，此字段为true，不代表三次都成功或日志全部落盘。

完整src/entry.cpp与patches/late-find-snapshot.diff等价；核验基线后在隔离副本git apply --unidiff-zero --check，通过后git apply --unidiff-zero。不在旧查询版R9或R7上强行套用。

## 2. 契约依据与限制

提供的本机SDK声明为ProError ProCmdCmdIdFind(char *cmd_name, uiCmdCmdId *cmd_id)。该头文件/帮助摘录没有明确禁止在action回调内调用Find；但也没有列出完整允许上下文，提供的官方样例不含Ping action内Find的直接范例。因此本候选是有真实签名支撑的受控只读观测，不能表述为官方明确保证该上下文可用。没有重新取得SDK原件进行独立核验，不以“没见禁令”替代运行验证。

按任务单复用已有Ping入口，不新增按钮、菜单、rbn或执行API，不把查询放进user_terminate。原三个Designate、注册参数、Access缓冲及六组消息探针保持原样；没有额外重复Designate或描述解析试验。旧事件revision保留，晚期事件使用独立R10标识。

p1s23核查是本机DAT/资源/class常量池结果：未发现名称/键/文件重叠，但不是完整源码/反编译证明。本候选不需要停用该应用，也不把并行运行本身当作冲突证据。

## 3. 预期信号与审核

本机助手按任务单核验包/源码哈希、真实SDK离线编译链接及四个入口导出/依赖，保持PRO_USE_VAR_ARGS在SDK头文件之前、既有库和两个plus强制引用。没有新SDK函数名称，新增运行调用只复用ProCmdCmdIdFind。不要修改厂商头文件、CRT、DAT、config.pro或资源；实际加载方式保持已有记录。

按任务单由用户在UI观察阶段使用已有Ping触发一次快照，不点击Body，不移动按钮，不打开、选择、写入或保存模型。本包不包含部署脚本或正式发布步骤。

同一pid/load_token应先有原R9三次初始化回查及基线，之后原READONLY_PING、三组晚期BEGIN/RETURN。后续Ping如因正常操作再次触发，应只有ALREADY_DONE而没有新的晚期Find；不要求为了验证门闩额外现场点击。

baseline_find_id/baseline_action_add_id/late_found_id均为十六进制JSON字符串；空指针为null。Find失败时late_found_id_null及两个比较为null，因为不能解释失败后的输出；成功空ID时空标志true、在有效基线下比较false。初始化基线不成立时两个比较均null，不把未知变成不相等。

probe_tick是Windows单调毫秒时钟，外层utc/seq仍来自原日志框架。probe_ordinal是整组三命令快照序号，不是单命令索引。原Access记录仍按原初始化/终止机制导出，不能把晚期Find触发与Access缓冲未落盘混淆。

填写acceptance.template.json并与同会话初始化事件离线比较。原R9已有真实现场结论不等于本R10已编译或运行通过。

## 4. 结果分支

- 构建失败：回传准确编译/链接诊断，不进入现场操作，不以强制转换或SDK替身通过检查。
- 晚期三项成功且ID与有效基线一致：削弱初始化后动作身份丢失/改变的解释；不证明中间从未变化、内部对象没被重建或ID没被复用，更不证明列表成员资格、消息路径或Ribbon分派正常。后续集中看定制上下文、placeability与UI配置关系。
- Body晚期查不到或ID不同：获得时序/上下文差异线索，先对照两个可见命令，不直接归因为注销、覆盖或根因。
- 三项均失败或出现其他原始码：记录晚期调用上下文/状态异常，不反推初始化失败，不映射为不可见状态。
- 初始化基线无效或尚未发布：保留原码/空值，比较为未知；不填入上一会话ID，不假定失败输出有效。
- BEGIN有RETURN缺失：根据command_ordinal定位未完成查询；不自动再点或重试。SDK异常/日志I/O失败可能造成不完整快照，ALREADY_DONE的complete标志也不能替代三条RETURN。
- 突然可见：保留时序事实，不把新增查询视为稳定修复。

Find只回查动作标识符；UI_membership_read_back固定false。指针值相等只是一致性观测，不作业务判断或初始化失败门。

## 5. 不做什么及验证范围

不增加Designate次数，不执行/触发查到的任何回调，不改动作/显示文字/消息资源/布局、注册顺序/priority/窗口标志/Access返回、图标或按钮位置；不新增菜单/Ribbon/rbn/刷新/选择/模型查询或转换代码，不停用p1s23。不更改入口签名、返回值、build/version/errbuf、CRT或既有链接契约，不自动操作Creo或部署正式版本。

云端完成补丁应用、逆向逐字节恢复R9、Designate调用仍为三处、终止中无Find、一次性原子claim无重置和固定三次上界的静态核查。纯C++ checks/late_find_format.cpp验证同ID/异ID/空ID/失败/无效或缺失基线及容量不足，使用真实可移植格式化代码，不含SDK替身。没有真实Windows门闩/并发测试、SDK编译、DLL加载或UI验证。
