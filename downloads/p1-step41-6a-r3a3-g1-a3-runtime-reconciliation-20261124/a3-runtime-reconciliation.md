# P1 Step41 6A R3A3 G1 A3 模型锁探针真机对账

2026-11-24 · 部署与真机回执对账完成，M1 守卫验证未完成。

## 真机结果

同一次操作 `operation_id=2` 覆盖 A4 和 A5，A1 至 A3 均未进入创建阶段。初始模型未锁且可修改，元素树就绪后 `ProMdlLockSet(true)` 返回 `-1 (PRO_TK_GENERAL_ERROR)`，回读 `is_locked=false`。探针按源码在 `A3_LOCK_NOT_CONFIRMED / a3_lock` 早停。回执记录 `create_called=false`、`create_status=null`、`regen_called=false`、`regen_status=null`、`save_called=false`。

清理时 `ProMdlLockSet(false)` 也返回 `-1`，故 `cleanup_failed=true`。随后 `ProMdlLockGet` 返回 0 且回读未锁，未观察到残留锁。回执记载模型树无新增特征，工作目录与归档副本身份一致。

M1 在此次 Creo 13.4.0.0 x64 本地零件会话不可观测。加锁调用的 `-1` 仅说明本次加锁未建立。锁定后 Create 是否返回 `-48 (PRO_TK_CANT_MODIFY)` 仍待观察，可按环境受限归档本次试验。

## 对账与勘误

- 部署清单、真机 JSON 和受控观察回执记录的六件部署文件身份一致。交接补丁校验通过，四份本地派生文本资源的字节数及 SHA-256 与部署记录逐项匹配。
- 隔离构建门据回执已通过。导出为 16 对 16，依赖为 8 对 8，导入 DLL 集合一致，记录新增导入符号 `CompareStringOrdinal`。
- 两份 JSON 的 `cases[A5].expectedStage` 固定为 `create`，应按发生清理时的操作阶段判定。此次 A4 在 `a3_lock` 退出并触发 A5，故保留 `actualStage=a3_lock`。
- 部署清单的 `runtimeObservation.note` 留有“待回滚”。终版真机 JSON 的 `closure.rollbackVerified` 已记录回滚，回滚前后基线为 2,775 字节且 SHA-256 一致。step23、R10 在位，探针配置行已移除。
- 中文按钮及消息乱码单列。冻结创建基线派生的中文转换资源为 UTF-8 无 BOM。此前 R3A3 加 BOM 的中文资源去掉 BOM 后，内容恰为 A3 资源的前缀。该差异可复核，现场生效消息语言和 Creo 解码行为仍待取证。

云端复核使用三份附件、本地候选源码及资源。原始 trace、截图、Windows DLL、dumpbin 输出、配置和模型文件由现场回执引用，本轮没有原件可供独立复算。
