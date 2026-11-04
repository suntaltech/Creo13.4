# R7 后续离线工作与受控准备资料

日期：2026-11-04。本目录仅整理资料、空白台账和门禁，不是R8软件候选，没有生成新ZIP、安装包或发布包。未修改R7、A1代码、注册配置或生产文件；未启动Creo、触发宏、采集GUI、创建/重生成/保存模型。

R7候选身份可核对，但实际加载源码、dat、classpath和现场状态仍UNVERIFIED。输入清单对R7本机审核的描述属于上游摘要，本轮未取得新的原始编译/运行证据，不将其改写为独立验证。

## 交付内容

- [空白观察台账](blank-observation-ledger.json)：C1–C4、C3三策略独立记录、12例负向记录、B七项前提和恢复前只读核对项。
- [计划与门禁](preparation-plan.json)：P-01至P-07及G0–G7顺序、授权边界。
- [证据勾稽与拒收](evidence-rules.md)：身份、requestId、截图/日志及操作者核对关系。
- [官方定位与未决索引](official-reference-index.md)：只保留官方文件名/主题，移除本机绝对路径。
- [A1稳定线回归准备](a1-stable-line-regression.md)：三种重命名规则、参数记忆和模式守卫，不合入P1候选。
- [生产恢复只读前置项](production-preflight.md)：不执行恢复、不提供配置片段或恢复命令。
- [资料清单](manifest.json)和SHA256SUMS：只验证本目录材料完整性，不认证真机证据。

## 下一步队列

| 阶段 | 目的 | 必要条件 | 本次状态 |
|---|---|---|---|
| G0 | 计划、字段、停止规则就绪 | 本目录校验及R7身份核对 | 仅离线准备 |
| G1 / C1、C2 | 单变量资源观察 | GUI受控授权、新会话、同一候选身份，只换layout | 未执行 |
| G2 / C3 | 需要时排查三个旧菜单策略 | 每策略独立会话与记录 | 未执行，旧API结果不等于L3 |
| G3 / L3 | 候选列表和搜索观察 | 手动来源、确切搜索/类别/模式、截图/日志及身份匹配 | UNVERIFIED |
| G4 / L4 | 手动Add、固定位置与反馈 | 另行Add授权及同会话手动操作/位置/反馈证据 | UNVERIFIED |
| G5 | 负向门禁与辅助服务观察 | 输入有界、预定停止点、逐项记录调用触及 | 未执行 |
| G6 / C4 | OTK备选路线前提核对 | 六项阻塞前提独立证据；不被C1/C2/C3替代 | 计划BLOCKED_PREREQUISITES |
| G7 / L5 | 未来受控副本转换 | UI/触发、资源基线、负向停止能力、路线选择及单独写模型授权 | 本次范围之外 |

这些是建议排队顺序，不是以一个层级结果推导另一个层级。C3是独立诊断分支，不能自动赋值L3；L4也不由L3自动成立。B是备选路径，其前提不由A或资源layout结果满足。

G5的12项包含11项业务门禁计划和1项CREOSON辅助服务观察；后者不是原生转换函数的失败分支。所有计划标签与实际错误码/阶段/完成分类分列。板厚取消、异常、原生框拦截、Java校验需真实区分，不预填。

## 每个会话必须分开的身份

1. 计划条件：plannedClass/plannedStartup/plannedLayout/plannedPolicyId，可以在离线列明。
2. 实际身份：源码/dat hash、classpath、模型模式、是否重启、commandId和requestId，本目录全部留空或UNVERIFIED。
3. 每次调用独立记录，旧三个命令和转换命令有各自requestId；只能按同一会话建立索引，不能拼接成一个请求的成功链路。
4. L3/L4和恢复生产、真实模型尝试均需对应独立授权；当前自动生成/上传离线材料的授权不覆盖这些动作。

## 使用方式

JSON仅为计划和可复制的空记录。按未来实际会话/命令克隆记录，保留原空白样本；known caseId/policyId放在计划字段，runtimeRecord仍未填。NO_ATTEMPT与C4阻断只是离线占位，不是实测没有调用或前提失败。

证据命名只约定组成，不给出虚构截图、日志文件名或requestId。源码/dat/session/command/请求及引用是否真正匹配由操作者核对；Schema和SHA清单不认证内容、许可或因果关系。

## C1/C2计划搜索词

下表从R7候选消息正文提取，仅是计划搜索输入，不表示原生消息已经解析、搜索已命中或A1稳定版使用相同标签。实际结果保持UNVERIFIED。

| commandId | 候选文本标签 |
|---|---|
| SISWBatchRename | Batch Rename |
| SISWSheetMetal | Sheet Metal Page |
| SISWAbout | About SISW |
| SISWSheetMetalConvert | Convert Solid to Sheet Metal 6A |

## 命名与克隆补充

[克隆来源索引](record-clone-index.json)与[规则衔接说明](naming-and-clone-guide.md)只列未来组织方式。没有创建九类现场目录、没有克隆运行记录，原空白台账和R7均未改。
