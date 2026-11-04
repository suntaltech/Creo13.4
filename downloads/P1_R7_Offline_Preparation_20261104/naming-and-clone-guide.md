# 目录、命名与克隆规则衔接说明

来源：用户提供《P1_R7_受控证据目录命名与克隆规则_20261104.md》。本轮只整理索引和规则，不创建未来证据目录，不克隆或填写运行记录，不生成截图、日志、requestId、配置或ZIP。

[克隆索引](record-clone-index.json)使用JSON Pointer对应[原始空白台账](blank-observation-ledger.json)。原台账、R7候选与schema均保持不变。

## 1. 九类目录是未来用途，不是本轮产物

01会话、02路线A、03 L3列表、04 L4固定按钮、05负向用例、06路线B、07保管、08恢复前只读核对、99审计索引分别对应索引中的directoryRole。实际目录只能在未来真实授权会话中建立。

建议先按唯一sessionScope隔离，再按九类用途分组。同一天、同操作者、同命令可能有多次会话或尝试；不能只用命令+日期区分，也不能覆盖旧记录。

保留原五段命名组成planId_or_caseId、condition、operatorRecordedDate、operatorIdentifier、recordKind；condition必须能区分layout/policy/输入条件、sessionScope与attemptOrdinal。这里仅列组成，不生成真实名称、日期、操作者标识或文件路径。

## 2. 克隆单位

| 类别 | 来源/数量 | 要点 |
|---|---|---|
| C1–C4 | sessionPlans / 4 | 一会话一副本，未执行项留空；C1/C2/C3不能满足C4 |
| C3策略 | independentRouteAPolicyRecords / 3 | 一个plannedPolicyId一份wrapper，实际填写其record；不继承其他策略或按钮结果 |
| L3/L4 | manualUiObservation / 原始空对象 | 各自从原空对象独立克隆；不要复制已填L3然后改名当L4 |
| 负向用例 | negativeCaseRows / 12 | expected计划元数据单独保留，实际只克隆runtimeRecord并按plannedCaseId关联 |
| OTK前提 | requirements与operator的prerequisites / 各7 | 按id匹配，要求元数据与实际操作者记录分开；六阻塞项和资源观测不混用 |
| 保管核对 | evidenceCustodyRecord / 空对象 | 一case/session/attempt范围一条；触发存在时才关联真实requestId |
| 恢复前核对 | productionReadonlyPreflight / 7 | 保持只读集合，actionExecuted=false不代表恢复授权 |
| 审计元数据 | 原ledger及其manifest/SHA | 保留原件，不被已填写副本覆盖 |

## 3. 展示字段与现有schema的映射

输入规则中的screenshotRef、logRef、manualOperationRef可作表格展示名，现有记录应使用evidenceRefs及kind=SCREENSHOT/LOG/MANUAL_OPERATION_RECORD，不把这些别名擅自添加成schema不接受的顶层字段。

prerequisiteId对应现有条目的id；requiredEvidenceKinds来自requirements元数据，不由operator结果推断。mustNotTouchTouched对应实际记录中的sdkCallTouches三项触及信息，不使用计划mustNotTouch列表替代观察。

会话、模式切换等详细上下文放到现有会话/保管记录或外部说明并引用；不随意向additionalProperties=false的观察对象加新字段。本轮没有修改schema枚举或字段。

## 4. 状态与停止规则

- 查无候选可以在外部观察说明或insufficientEvidenceReasons中表达NOT_FOUND；不能直接把它写入现有l3CandidateListVisible状态枚举。已有状态为UNVERIFIED/PASS/FAILED/BLOCKED，须按真实证据和适用规则填写。
- 原规则“在预定门禁前继续执行”应按“越过预定门禁仍继续”理解；正常到达门禁的前序步骤不应误判为失效。若越过停止点或观察到禁止写调用，停止后续流程并保留实际状态。
- L1只有对象解析、尚无回调时，不制造requestId。L2与负向回调存在时才以实际requestId关联错误、阶段和完成分类，不能跨请求拼接。
- L3不推出L4，旧菜单返回不推出L3，C3不推出C4；Schema形状通过不代替保管完整性核对。
- 原始运行材料不因不匹配而删除；隔离并写明拒收/待核对原因，不补造成功值或未调用值。

## 5. 不在本轮执行

不启动Creo、不切dat、不触发宏、不操作模型、不手动Add、不恢复生产配置、不导出或手写rbn。后续自动生成离线资料的授权不覆盖这些动作。
