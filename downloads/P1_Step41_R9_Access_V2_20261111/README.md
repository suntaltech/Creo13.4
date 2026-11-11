# Step41 R9 Access V2：命令回查与有界Access观测

日期：2026-11-11（北京时间）。按新任务单扩展旧R9查询版。这是离线只读诊断候选，不是正式插件版本或转换实现。

## 1. 文件与基线

仅src/entry.cpp变化，diff基于指定R7源码：8ecf88e80d366d7053a2c14475dff4393bd6b1fc1770f2f6c6d96fccb6e3faf9。完整源码与patches/r7-to-r9-access-v2.diff等价；隔离副本核对基线后git apply --unidiff-zero --check，通过后git apply --unidiff-zero。

不要把本包与旧P1_Step41_R9_Command_Lookup_20261111混用；旧包只有ID查询，本包revision为UI_R9_ACCESS_V2，包含Access wrapper及新查询字段。对当前R7直接替换完整源码即可，不要求本机助手手改C++。

资源仍用既有R5/R7六份文件，消息键/值、标签、语言布局均不改。旧六组消息探针保留、不增加或重复调用；它们不是本轮新增的截断试验。

## 2. 两个观测点

每个ActionAdd成功且Designate调用返回后，按同一SDK动作名查找一次ID。新增CMD_ID_FIND_PROBE_BEGIN/RETURN，记录command、sdk_action_name、action_add_id、found_id、find_pro_error、found_id_null、id_match和designate_pro_error。ID不解引用、释放或执行；十六进制字符串避免整数精度损失。

成功但返回空ID时found_id=null、found_id_null=true、id_match=false。Find失败时不解释输出，found_id/null标志/id_match都记null，同时保留原错误码；这是任务建议字段中必要的有效性区分，不能把失败后的初始化值冒称SDK返回空ID。Find比较仅作观测，不改变注册、可用性或初始化返回。

三个注册函数分别挂ping_access_observed、model_access_observed、body_access_observed。它们都先调用原readonly_ping_access(mode)，记录结果后返回原值，仍是ACCESS_AVAILABLE。返回逻辑、priority和两个窗口标志不改；新函数指针通过真实uiCmdAccessFn类型检查，不做函数指针强转。

## 3. Access高频路径与落盘时机

为满足不阻塞要求，wrapper不调用trace_event、文件/调试器I/O、SDK查询、模型访问、堆分配、锁或重试循环。仅原返回逻辑、GetLastError保存恢复、Interlocked计数/发布和GetTickCount64打点。每命令每次DLL加载保存前64条完整样本（共最多192条），超过后仍计总次数及MODE_BLIND/MODE_ERROR_MSG_ALLOWED/其他mode次数，不记录无限明细。

实际落盘沿用原JSONL容错逻辑，位于初始化末尾（原消息探针之后）和已有user_terminate中：

- ACCESS_CALL_OBSERVED：command、sdk_action_name、access_mode整数、实际access_returned整数、call_ordinal、原调用access_call_tick、flush_phase、buffered=true。
- ACCESS_TRACE_SUMMARY：总次数、三类mode计数、明细上限、此次尝试发出的样本数、尚未发布的槽数、超过上限而未保存的明细数。

记录的外层utc/seq是落盘时间，不是回调时间；access_call_tick是Windows单调毫秒时钟。状态槽用原子发布/领取，每个已发布样本最多尝试落盘一次，读写不同时改同一已发布样本。摘要是多个原子计数的快照而非整体事务，snapshot_is_atomic=false；并发时各计数短暂不一致或存在未发布样本不等于数据伪造。

日志I/O失败仍由既有TRACE_IO_FAILED信号处理，不自动重试；samples_emitted_in_flush表示调用日志器的尝试数，不保证磁盘写入成功。不可把缺失记录等同于没有回调。

没有新后台线程、定时器、通知注册、导出按钮或SDK API用于flush；总明细有上限，落盘循环最多64槽/命令。

## 4. 本机验收与结果解释

按任务单先核验ZIP/内部清单/源码哈希，使用既有真实SDK离线编译链接，记录诊断并核对四个正常/plus导出及依赖。保持PRO_USE_VAR_ARGS先于SDK头文件、两个plus强制引用、既有库/CRT/入口/DAT契约，不修改厂商头文件。云端没有真实Windows SDK，原子缓冲的并发行为和Windows编译需本机验证，不能用替身SDK编过代替。

用户只按已有受控流程启动候选、打开同一TOOLKIT定制列表并截图，不点击面计数、不打开/选择/保存模型，不为本轮编辑config.pro。记录实际加载方式，保持当前条件，不重测已关闭分支。

初始化末尾三条summary给出打开列表前的基线。若既有受控流程随后允许用户手动停止该辅助应用，则关闭定制对话框后按该流程停止，收集TERM_ENTER/termination阶段三条summary和TERM_BEFORE_RETURN，再交回完整同一pid/load_token日志。不要由脚本或插件自动停止Creo。

如果本轮范围只允许启动和打开列表、尚不允许停止，则不新增停止动作：只回传现有初始化/截图，并明确标记post_UI_access_flush_available=false。此时缓冲内的后续Access证据尚未导出，必须保持“未知”，不能给出body未被调用的阴性结论。异常退出、未执行终止、日志写失败或未发布样本同样限制结论。

用初始化与终止累计计数差分观察这段运行期间是否有各命令回调；这段期间包括打开/关闭界面等活动，不是精确的“列表构建时刻”事件。没有SDK文档保证列表填充必调用Access，不能从可见命令有调用而body无调用直接证明某个内部过滤阶段。

判定分支：

- 三个Find成功且ID匹配：当前按名身份可查，不代表UI列表成员资格。
- Find失败/ID不同/空：记录事实与两个对照，不推断已释放，不重新注册或执行ID；文档未显式保证ID相等。
- 三个wrapper都有记录、都返回ACCESS_AVAILABLE而body仍缺失：观测到相同可用性返回，但不是所有UI内部状态的回读。
- 对照有调用而body累计为0：只说明观测窗口内未见body wrapper调用，支持进一步分析分派差异，不直接认定过滤发生在某一步。
- 全部0：可能窗口内未触发Access，不能当成插件未注册或列表已调用但拒绝。
- total_calls>64：详细样本不完整，以计数和mode汇总为准；没有后续详细mode样本不等于没发生。
- observed access_returned与ACCESS_AVAILABLE不符：先核对源码/DLL与事件身份；本包共用原返回函数，不会按命令改状态。
- BEGIN有RETURN缺失：定位最后动作名，不自动重试。真实SDK编译失败则停止现场操作。

填写acceptance.template.json。回调触发标志仍按精确BODY_FACE_COUNT_ENTER/RETURN匹配，REGISTER_BEGIN和ACCESS_CALL_OBSERVED都不是业务回调执行证据。

## 5. 不做什么与云端检查

不改动作名、显示标签、帮助/描述、消息键/文件名/格式、资源布局、注册顺序/优先级/窗口标志、Access返回值、业务计数或六组原消息调用。不新增rbn、菜单、图标、刷新、别名、消息弹窗或命令执行；不读几何、不选面、不写入/保存/转换模型、不自动操作Creo或修改配置/缓存，不部署正式版本。

唯一新增SDK函数是ProCmdCmdIdFind；Access包装只用已有回调契约与Windows原子/时钟操作。云端完成R7基线补丁应用、移除Access扩展后恢复已审核查询候选、回调路径无I/O/循环/分配/SDK查询的静态检查，以及纯C++新查询日志格式的同ID/异ID/空ID/失败/容量不足测试。没有真实SDK构建、Access并发运行或UI验收。本包是可审核源码交接包，不是正式打包发布或功能通过结论。
