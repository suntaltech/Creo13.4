# 官方资料文件名与未决问题索引

来源：用户提供《P1_R7_官方资料定位与未决问题索引_20261104.md》。标记：OFFICIAL_DOCUMENT_LOCATOR_FROM_USER_SUPPLIED_INDEX_NOT_INDEPENDENT_RESEARCH。

本轮未访问本机HTML或独立检索官方原文；下面保留文件名、主题和上游摘要，删除所有本机绝对路径。不把资料摘录升级为运行、许可或发布结论。

| 文件名/主题 | 输入资料给出的方向 | 仍需核对 |
|---|---|---|
| prosmtdrvsurf_h.html / driving surface | 驱动面树使用PRO_FEAT_DATUM_SURF、PRO_DTM_SRF_AS_WALL与PRO_E_SMT_DRV_SURF | 材料侧、切换方向、单位、创建错误与回读 |
| prosmtshell_h.html / shell对照 | shell使用PRO_E_SHELL_SRF，方向与driving方案不同 | 不混入当前MVP，未来须独立评估 |
| proelemid_h.html / 元素字典 | 核对驱动面、板厚、材料侧和切换侧元素语义 | 不依据名称猜测新节点、类型或值 |
| profeattype_h.html / C特征类型 | 输入索引列C侧PRO_FEAT_DATUM_SURF=942 | 不与Java枚举整数混用，不把C值直接写入Java树 |
| c-pfcFeature-FeatureType.html / Java枚举 | 使用SDK枚举语义；输入索引称FEATTYPE_SHEETMETAL_CONVERSION偏向NC-SHEETMETAL | 不因名称或154编号改现有driving根类型；实际对象回读待证 |
| Designating_the_Command.html / 命令暴露 | Designate与候选列表、标签及消息文件有关；必要条件不是充分证据 | startup java的真实列表/搜索/固定位置 |
| Compatibility_with_J_Link.html / OTK兼容 | 应用JAR、官方SDK classpath、otk_java身份等前提 | 实际JAR、classpath与加载条件 |
| Unlocking_Running_and_Signing_the_Creo_Object_TOOLKIT_Java_Application.html / 解锁运行签名 | 官方流程及开发/运行许可条件 | 操作者真实证据；本目录不生成命令或满足结论 |
| Registry_File.html、RegistryFile1.html、Registry_File_2.html、Registry_File_Data.html / 注册 | 核对startup、toolkit、入口、classpath与text_dir身份 | 真正生效字段、资源搜索、加载日志 |

driving子类型15、shell对照24及C/Java枚举说明均来自输入索引，不是本轮重新核验的SDK值。当前不修改树、不改为shell、不凭枚举名或整数宣称可创建。

## 未决问题排队

| 编号 | 问题 | 所需外部证据 | 离线准备入口 |
|---|---|---|---|
| Q1 | startup java + Designate能否进入列表 | 同会话对象、手动列表/搜索、类别/模式、截图/日志 | C1/C2、手动UI记录 |
| Q2 | 哪种资源layout被解析 | 新会话/重启、键查找、状态栏与异常 | C1/C2 |
| Q3 | null参数消息调用如何表现 | 原始返回/异常、requestId、实际文本 | 反馈观察 |
| Q4 | 三个旧菜单策略各自结果 | 每策略独立身份、调用结果和异常 | C3三个单独记录 |
| Q5 | 手动Add后是否固定且有反馈 | 独立授权、操作/位置/点击记录 | L4 |
| Q6 | 各门禁实际停止点 | 实际错误/阶段/完成分类及调用触及 | 12例台账 |
| Q7 | OTK六项阻塞前提是否满足 | JAR/classpath/解锁/签名/许可/注册原始记录 | C4七项表 |
| Q8 | driving转换能否完整回读 | 创建、重生成、钣金状态、特征身份/状态 | 前置齐备后另行授权的L5 |
| Q9 | 材料侧/切换侧是否需显式节点 | 官方样例、实际创建错误或树回读 | 保留未决，不提前改树 |
| Q10 | 历史受控配置何时恢复 | 备份/当前比对与独立授权 | 生产只读前置表 |

原始结果与索引摘要不一致时，先核对官方原文、版本与环境，再修订适用范围；不因一次异常猜测新元素值或替代许可判断。资料定位不代替真实证据。
