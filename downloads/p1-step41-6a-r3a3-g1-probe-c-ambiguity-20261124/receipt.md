# P1 Step41 6A R3A3 G1 探针 C · 歧义选取交接回执

2026-11-24 · 探针 B 源码基线 `70469a236bfcb246f76672db3017b988db719014` · Windows SDK 构建和 Creo 真机待验

## 改动和证据

仅修改 `src/smt_driving_command.inl`。按同版 `ProSelection.h:453`，保持 `surface`，将 `ProSelect` 的上限从 1 调为 2；实际选取数量仍由 SDK 输出。成功但多选时沿用既有 `SELECTION_AMBIGUOUS/select` 和 `P1_SMT_CONV_ONE_FACE`，返回前在借用生命周期内用 `ProSelectionModelitemGet` 读取选中项身份。最多查看配置上限个选中项，既不创建副本，也不释放借用数组。空指针、查询失败或诊断异常只影响取证字段。单选仍仅复制一个有效选中项，按原逻辑检查面并释放自有副本。

| 事件 | 字段和口径 |
|---|---|
| `SMT_CONV_API_RETURN` | 既有 API 名更新为 `ProSelect(surface,2)`，继续记录原始 SDK 返回码。 |
| `SMT_CONV_SELECTION` | 按 `operation_id` 输出 `stage=select`、`filter=surface`、`max_count=2`、原始 `pro_error`。成功时 `count` 为返回实数，SDK 非成功时为 null。 |
| `SMT_CONV_AMBIGUOUS_ITEM` | 仅在 SDK 成功且多选时输出。每项记录从 0 起算的 `selection_index`、`item_id`、`item_type`、`owner_matches_model`、查询返回码和异常标记。身份字段仅在读取成功时有效。 |
| `SMT_CONV_RETURN` | 终态字段及结果码规则与探针 B 一致。SDK 中止为 `CANCELLED`，其他非成功为 `SELECTION_FAILED`，成功返回 0 项为 `NO_SELECTED_FACE`。多选在板厚/建树/创建/重生成前以 `SELECTION_AMBIGUOUS` 返回。 |

替身验证覆盖双选、有效单选、零选、取消、SDK 失败、空数组、空选中项和身份查询异常。双选的记录包含两个身份，Copy、Free 和后续业务计数均为 0。有效单选 Copy/Free 各一次。冻结 B 源码副本上已核验补丁应用和反向还原；七份文件清单中仅上述 inl 的哈希变化。四份消息资源保持原样，包括 R3A3 的两份带 BOM 中文资源。没有新增模型写调用、资源键或命令。

## 隔离构建与预登记

优先解压源码 ZIP 到新的 C 隔离目录。ZIP 内含派生源码、四份资源、清单、补丁和回执。若使用单独补丁，只在解压到新位置的已校验探针 B 源码副本中执行 `git apply --whitespace=error --check <diff>` 后再应用。冻结 B 源码和现有现场目录各自留档。

复用探针 B 的已核验 MSVC、Creo 13.4 SDK、链接库和入口，构建隔离的 `src\entry.cpp`，保留 `/MT /EHsc /MACHINE:X64 /INCREMENTAL:NO` 和原两个 `/INCLUDE`。建议新 DLL 名 `p1_step41_cpp6a_g1_probe_c_ambiguity.dll`，新 DAT 名 `p1_step41_cpp6a_g1_probe_c_ambiguity.dat`，注册名 `p1_step41_g1_probe_c_ambiguity`。本机编写并核验独立 DAT 中的 `exec_file` 和 `text_dir`，均指向 C 隔离位置；真实 DLL/DAT 字节与 SHA-256 填入清单的预登记项。转换 action 名继续沿用原候选，受控切换时避免与 B 或正式候选同时激活，保留 Step23 对照。

本机按实际 cl/link 退出码和 `dumpbin /exports`、`/dependents`、`/imports` 对照冻结基线和探针 B。源码侧未引入新的导出声明或链接库。未取得真实 Windows SDK 构建或 dumpbin 记录，编译、链接、实际符号集合及 DLL/DAT 哈希留空。本轮 C++14 替身与静态核验不替代上述门。

## 真机判定

经单独授权并核对 C DLL/DAT 身份后，对只读打开的单件选两个可由 `surface` 过滤器接受的曲面。BOLT 仅是待核验样件。按同一进程、加载实例和操作 ID 核对选取记录、身份事件和终态。实测通过需 SDK 成功返回 `count≥2`、终态 `SELECTION_AMBIGUOUS/select`、`model_may_be_modified=false`、`save_called=false`，并确认无 Copy 或后续阶段。单选对照在板厚提示处取消。回执缺必要事件时保留 UNVERIFIED；若 GUI 只返回一项或取消，按实际终态登记。收尾依已有受控流程退出、回滚并核对配置身份。
