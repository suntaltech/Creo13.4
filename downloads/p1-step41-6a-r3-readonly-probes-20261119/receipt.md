# P1 Step41 6A R3 只读探针 A/B/C 交接回执

2026-11-19 · Creo 13.4.0.0 · 冻结创建候选 `b99692e3f927ff297941e812e36ac5646bd323b7`

## 交付与隔离

| 产物 | 用途 |
|---|---|
| `p1-step41-6a-r3-readonly-probes-source.zip` | 可直接解压到 Windows 隔离目录的源码、独立消息资源和清单。SHA-256 为 `747a9be7ed2e16fbcc50dfd790143a13baee7ebcfdcd99a542748b79f9369a82`。 |
| `p1-step41-6a-r3-readonly-probes.diff` | 对冻结创建源码副本可追溯的 Git 补丁，CP936 资源使用 Git 二进制补丁表示。SHA-256 为 `768e3aeb7a8cd7583811a07e446443fc15eaa188a7eca580c6f8c06f0f2839b1`。 |
| `manifest.json` | 冻结输入、补丁及九个源码/资源文件的字节数与 SHA-256。 |

使用源码 ZIP 时，无需另外应用 diff。使用 diff 时，在含冻结 `src\`、`text\` 的临时副本运行 `git apply --whitespace=error --check <patch>`，通过后再应用；构建文件以清单为准。原创建命令和树源文件仍是独立冻结基线，ZIP 的编译入口仅包含只读探针，不带创建链。三条既有只读命令的注册顺序、回调、access 和 trace 辅助保持原样。若要运行这三条对照命令，另用原本机已核验的三套资源，保留其字节。

本轮未生成 Windows SDK 构建或运行产物。源码静态核对包含补丁应用与反向还原、ZIP CRC 及清单、资源编码和原 usascii 哈希、创建链隔离以及禁用调用扫描。Linux C++14 替身核验覆盖 A 的样本顺序和静态失败后跳过动态参数，B 的 1 mm 样本换算，以及 C 的分批面 ID、唯一归属和收集失败时释放非空数组；这些替身结果仅用于验证候选自身的逻辑和日志。

## A · 中文消息解码

命令 `p1_r3_probe_msg` 每触发一次显示一个样本。第一轮的触发顺序如下；第十个样本后回到控制组。

| 触发编号 | 资源 | 样本 |
|---|---|---|
| 1 | 字节与冻结 usascii 完全一样的独立文件，两种语言目录均有同一副本 | 同键 ASCII 静态对照 |
| 2–4 | UTF-8 无 BOM，字节与冻结中文资源相同 | 静态 → ASCII 动态 → 中文动态 |
| 5–7 | UTF-8 有 BOM，正文与上组相同 | 静态 → ASCII 动态 → 中文动态 |
| 8–10 | CP936 无 BOM，内容、键及记录结构与 UTF-8 无 BOM 对照一致 | 静态 → ASCII 动态 → 中文动态 |

每一变体的动态样本要求其静态缓冲完全匹配。中文动态样本还要求该变体的 ASCII 动态缓冲匹配。跳过的样本记录 `PREREQUISITE_UNVERIFIED` 和空 SDK 调用值，状态栏可能保留上一条消息，截图勿归给跳过样本。中文动态参数按现有候选的 UTF-8 窄字符串方式传递，用于测量 SDK 解释结果。ZIP 中 UTF-8 BOM 与 CP936 是诊断对照，尚无编码修复结论。

使用同一 `operation_id` 的 `PROBE_MSG_BEGIN/RETURN` 和用户截图回填。RETURN 给出预期与实际缓冲 UTF-16、是否终止、比较结果、显示调用结果及参数结果。显示字段在原始 trace 为 null，由截图独立填判。控制哈希是经离线核验的资源登记值。

## B · inch 单位换算

命令 `p1_r3_probe_units` 仅取当前零件、主单位制、实际长度单位与 mm 基础单位，用 `ProUnitConversionCalculate(&mm, &target, …)` 算 1.0 mm 样本。失败时保留原 SDK 码、空/无效名称及已取得的因子信息。用 mm 零件作对照，再用已经确认长度单位为 inch 的普通零件比对，inch 样本应约为 0.03937007874015748，offset 为 0。以 `PROBE_UNITS_BEGIN/RETURN` 配对，结合模型实际单位和记录的单位名称判定。

## C · 多体面归属

命令 `p1_r3_probe_bodies` 枚举体数组，逐体查表面 ID，再释放 SDK 数组。数组收集失败但返回非空时也释放，记录实际 `ProArrayFree` 返回码。每体面 ID 按最多 32 个一段记录，逐体概要另有 `surface_ids_complete`。选面只读取 SDK 借用选择项的 modelitem；空选、取消、非平面、owner 不一致或上下文变动均停止。

先准备一个已经在 Creo 中确认有两个独立实体体的零件。按提示先选默认体一平面，再选另一体一平面。核对 `PROBE_BODIES_BEGIN/RETURN`、逐体遍历和两条 `R3_PROBE_BODY_MATCH`。每个所选 ID 必须恰有一次匹配，且默认体角色与预期一致。遍历失败、无匹配、多匹配或角色不符保守判为探针的 `FACE_BODY_MISMATCH`，与生产转换终态分开。两次拉伸形成同一体的样件仅能作为单体对照。

## 本机四门和现场边界

先核对旁置校验及清单。复用冻结候选的本机 VS2022 17.14.41、MSVC 14.44.35207、Windows SDK 10.0.26100.0、Creo 13.4 SDK 和原链接库，编译隔离目录的 `src\entry.cpp`，保留 `/MT /EHsc /MACHINE:X64 /INCREMENTAL:NO` 及原两个 `/INCLUDE` 入口。产物使用独立名称（建议 `p1_step41_cpp6a_r3_readonly_probes.dll`）。由本机记录真实退出码、dumpbin 四入口和依赖差异，登记 DLL、DAT 与资源身份后再讨论真机。

真机按 A → B → C 组织。对照组先行，目标件、注册和截图由用户在授权会话完成。API START/DONE 的 null 表示未返回状态，不能读成 0。采集独立探针 BEGIN/RETURN、原始 SDK 码、trace 行数、截图和关闭后的证据。inch 零件和真多体零件仍待确认。
