# P1 Step41 C++ 6A R2 B 类单位反馈探针交接回执

2026-11-18 · 源码静态核对完成，待本机 SDK 编译及真机验证。

## 候选与基线

候选命令为 `p1_smt_unit_probe`。它在通过 R2 的普通零件、默认体、选面、板厚和上下文重验后，查询当前长度单位，记录换算因子，计算示例板厚，并以 `UNIT_PROBE_STOPPED` 返回。独立候选中没有元素树实现、特征创建、重生成或创建后回读。R2 冻结基线和既有三条只读命令保持原样。

新版完整输入 ZIP 的 SHA-256 为 `9221aca8c27c6163c624dac3e1810a102101b772f8c6a1c96a2d728a910d0b63`，31 个登记文件核验通过。该值对应新版完整输入包。此前公开下载的 2967 字节 ZIP 是回执副本，SHA-256 为 `c4450df92a20c22daf412d0c527b03bc7a0fa1b28ebee19cad49c4183616cae9`。

| 交付项 | SHA-256 |
|---|---|
| `cpp-6a-r2-b-unit-probe.diff` | `9bec04a754cc03d131e9e34d3463825cd756ffa0f9c1fa9bd421aa47cae7b6ae` |
| 派生 `src/entry.cpp` | `8d0783ec5b2048d25aa4c9f45f73676b5d55e1c5f565ca41cfcc0413b87f7d40` |
| 派生 `src/smt_driving_command.inl` | `f6db7a2b78c4f70cea85b15030c26d408268420757d4615153284f85d21b6f6e` |
| 派生 `text/chinese_cn/p1_smt_unit_probe.txt` | `8cad94c1fc3f50b6791fe05ef56e0f61f240631e489c6de00b1ff2f2613ab833` |
| 派生 `text/usascii/p1_smt_unit_probe.txt` | `49a989770b876ab6a170e5d7997679614a811b84a392e8d8f073ea86e1c9b3c3` |

## 补丁内容与静态核对

| 文件 | 变动 |
|---|---|
| `src/entry.cpp` | 三条既有只读命令仍按原顺序注册，随后注册探针。移除树实现 include 及树专用头。保留既有只读逻辑及共享 trace、access 包装。 |
| `src/smt_driving_command.inl` | 保留原有守卫、输入、取消、上下文重验与 SDK 单位调用。终态携带单位字段。硬停替换树准备、创建、重生成及创建后回读。 |
| `src/smt_driving_tree.inl` | 从派生源码删除。 |
| `text/chinese_cn/` 和 `text/usascii/` | 替换为 `p1_smt_unit_probe.txt` 和独立 `P1_SMT_UNIT_*` 键。两种资源的 22 个键与源码双向匹配。 |

在冻结 R2 的临时副本上实跑补丁应用和反向还原，回算哈希全部一致。两份派生可编译源码静态扫描中，`ProFeatureWithoptionsCreate`、`ProSolidRegenerate`、旧创建入口 `ProFeatureCreate`、树构造与保存调用均为零。保留的两处钣金查询用于初始守卫与上下文重验。检查采用源码对比、调用名扫描及日志格式串解析，未执行候选代码或 SDK。

## Windows 本机离线门

先复核下载的补丁及六个冻结基线条目的 SHA-256。把完整交接包的 `baseline/` 复制到新的隔离目录，在该副本根目录运行以下 Git 命令。两种 TOOLKIT 消息资源沿用四行一组的格式，命令保留各文件末尾的空分隔行。

```bash
git -c core.whitespace=-blank-at-eof apply --whitespace=error --check /path/to/cpp-6a-r2-b-unit-probe.diff
git -c core.whitespace=-blank-at-eof apply --whitespace=error /path/to/cpp-6a-r2-b-unit-probe.diff
```

复用已有 R2 `r2-build.cmd` 的隔离编译和链接口径，将输入翻译单元定位到副本 `src/entry.cpp`，将输出定位到该副本的 `build/`，DLL 命名为 `p1_step41_cpp6a_r2_b_unit_probe.dll`。沿用 MSVC 14.44.35207、Windows SDK 10.0.26100.0、`/MT /EHsc`、`/MACHINE:X64 /INCREMENTAL:NO`、R2 的预处理定义、真实 13.4 SDK includes、原库顺序及两个 `/INCLUDE` 入口。保留编译和链接的实测退出码与日志。

用 `dumpbin /exports`、`/dependents`、`/imports` 分别归档。核对四个必需入口 `user_initialize`、`user_initialize_plus`、`user_terminate`、`user_terminate_plus`，并对账 R10 的依赖 DLL 集合。记录实际 imports/exports 差异及证据清单。可另查 `entry.obj` 的外部符号以交叉核对源码扫描。此回执无编译、链接、dumpbin 或 SDK 执行结果。

## 真机验收预期

离线门通过并获单独授权后，由本机助手收集同一进程、加载实例及 `operation_id` 的事件，用户在 GUI 手动执行观察。

| 用例 | 预期 |
|---|---|
| 无模型、装配或绘图、无默认体、已是钣金 | 对应守卫拒绝，未进入单位阶段。 |
| 未选面、无效或非平面、多个返回项或默认体不匹配 | 对应 R2 拒绝分支。实际 preselection 数组行为待实测。 |
| 选面取消或板厚取消 | SDK 原始返回分别为 `PRO_TK_USER_ABORT` 和 `PRO_TK_MSG_USER_QUIT`，终态为 `CANCELLED`。 |
| 无效板厚、格式化失败、上下文变化 | 分别记录 `INVALID_THICKNESS`、`INPUT_FORMAT_FAILED`、`STALE_CONTEXT`。输入提示失败为 `FEEDBACK_NOT_READY`。 |
| 单位查询失败、因子非法或单位反馈无法编码 | `UNIT_UNSUPPORTED`。参照对应 API 原始返回码；其中因子或编码校验失败可能伴随 SDK 返回成功。 |
| 平面与正板厚通过，分别在 mm 与 inch 模型观察 | `SMT_CONV_UNITS` 和 `SMT_CONV_RETURN` 以同一 ID 配对，四个单位字段逐项一致，终态为 `UNIT_PROBE_STOPPED`。对照模型当前长度单位与参考换算。mm 到 mm 的参考 scale 为 1，mm 到 inch 的参考 scale 为 1/25.4，长度 offset 的参考值为 0。记录 SDK 实测值和实际单位名称。 |
| 命令异常或并发触发 | 终态分别为 `UNEXPECTED_EXCEPTION` 或 `COMMAND_BUSY`。 |
| 所有终态 | `create_called=false`、`create_status=null`、`regen_called=false`、`regen_status=null`。这些字面量表示候选不存在相应调用或状态。缺少终态，或成功用例缺少配对单位事件时，保留为未验证。可见性、资源显示及三条只读命令同会话响应仍待实测。 |

现有日志通道的写入失败按原策略报告。真机判定以实际配对日志为准。正向创建另列授权。
