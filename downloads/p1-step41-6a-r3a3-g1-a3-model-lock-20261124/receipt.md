# P1 Step41 6A R3A3 G1 A3 模型锁探针离线回执

日期为 2026-11-24。状态为源码候选，待 Windows SDK 编译、链接、dumpbin 和 Creo 真机验证。

## 选型与实现

采用形态甲。基线为冻结的 6A 创建候选，源码哈希已与创建候选清单对账。探针在隔离构建中使用独立命令 `p1_smt_a3_lock_probe`，其余三条只读命令由原 `entry.cpp` 注册。补丁沿用基线选面、板厚、单位、树准备、创建参数和错误码判定。支持原有两种语言资源，保留原消息内容和文件编码。

目标限定为已独立准备的可丢弃零件副本 `BOLT_A3_LOCK_PROBE`，名称匹配忽略大小写。部署前仍需确认副本目录、活动模型身份和配置，名称检查只验证会话模型名。获得真机授权后才可执行，原件保持只读打开。该候选会改变副本会话锁状态，并可能在副本内存中创建特征。

选面、输入、反馈预检及树准备通过后，记录初始锁状态与可修改性，尝试模型锁定并回读。初始锁必须明确为未锁，锁定调用必须成功且回读为真，才执行一次现有创建调用。可修改性查询作为诊断。恢复动作仅作用于本操作尝试加锁的模型，并回读复核。探针在 Create 返回后结束创建流程，任何路径均无显式再生、删除或保存调用。创建成功时应丢弃副本会话并记录树状态。形态乙和解锁后创建对照仅保留在评估中，需独立安排。

## 真机判据

按同一 `operation_id` 对账 `SMT_CONV_A3_LOCK`、`SMT_CONV_API_START/DONE`、`SMT_CONV_ERROR_LIST` / `SMT_CONV_ELEMENT_ERROR`、`SMT_CONV_RETURN` 和树截图。

| 观测 | 记账 |
| --- | --- |
| 加锁返回 0、回读 true、Create 返回 -48、终态 `MODEL_READ_ONLY/create`、无新特征、未显式再生或保存、解锁成功并回读 false | A3 守卫命中。终态应有 `model_may_be_modified=false` 和 `cleanup_failed=false` |
| 加锁确认后 Create 返回 0 | 本次模型锁条件下创建成功，记录 `A3_CREATE_SUCCEEDED_DISCARD_COPY`，采证后关闭并丢弃副本，不保存 |
| 加锁确认后 Create 返回其他错误 | 记录 `CREATE_FEATURE_FAILED/create` 与实际错误。若按评估口径归档 `NOT_CONSTRUCTIBLE/OUT_OF_SCOPE`，结论限定到本次环境和机制 |
| 未进入 Create，或 Create 抛异常无返回码 | 本次未完成守卫验证。保留调用事实与原始阶段 |
| 解锁失败、解锁回读失败或仍报告已锁 | 保留创建结果，同时标记收尾失败。停止在该会话继续试验，按受控副本退出流程处置 |

`create_called=false` 时 `create_status=null`。调用抛出异常时可出现 `create_called=true` 且 `create_status=null`，用 API 日志区分。日志中的 `regen_called=false` 描述候选未显式调用再生接口，树截图用于验证实际模型状态。

Linux 已通过补丁正向和反向应用检查、文件逐字节比较、及基于 C++14 SDK 模拟的十种情形。评估单记录的 SDK 接口和语义作为 API 依据，本轮未独立访问 PTC 在线帮助或用官方头文件编译。未部署、运行 Creo、写模型、打 ZIP 或发布。
