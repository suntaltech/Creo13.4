# 原生只读几何第一步：真机验证审核

日期：2026-11-05。审核native-readonly-step1-verify-20261105.zip。只解析上传JSONL并查看截图，不执行新的SDK模型读取、配置、GUI或部署操作。

## 结论

核心几何读取能力已有真机材料支持：1个实体、222个唯一曲面，实体状态、面类型、有效面积和体关联字段均取得。本步骤的实体和曲面字段覆盖率与原日志一致，能够由逐条记录复算。整体仍应保留PARTIAL，不改写为全部成功，因为原生revision缺失导致身份核对条件未满足。

这不是采集崩溃或类型/面积读取失败：errors=0、未截断、耗时约197毫秒；UNKNOWN按设计保留，没有补造revision或绝对精度。单次时长不代表大模型性能或30秒硬超时能力。本轮可冻结第一步候选，继续规划第二步采样。

## 已核对的读取结果

| 项目 | 结果 |
|---|---|
| 默认/枚举实体 | 同一bodyId=-5778，1个实体 |
| 原生IsSheetmetal | false；只代表实体原生状态，不等于非薄壁 |
| 曲面记录 | 222个唯一ID、全部属于本次枚举模型集合 |
| 真实GetSurfaceType | PLANE 126，CYLINDER 96 |
| 体关联及类型/面积字段 | 222/222完整；关联来源为Body.ListSurfaces成员关系 |
| 长度单位 | mm，表达式mm |
| 相对精度getter | 约0.0012 |
| 绝对精度getter | 返回null，保留UNKNOWN |
| GetRevision | 前、原模型后、当前模型后三次均null |
| 前后GetIsModified | 均false；活动模型原生句柄一致 |

bodyId为负值不应擅自判成无效或改为正数；它是本次SDK返回的标识，成员关系记录也一致。面积为正有限值，类型代码0/1分别对应PLANE/CYLINDER；不以特征名或包络候选代替类型。

## 覆盖率与PARTIAL原因

JSONL共234条、同一个真实requestId=58e89626-61ee-4c98-ab0c-8f74e1cdec10；开始UTC07:09:38.131622700Z（北京时间15:09:38），终结约197毫秒后。存在唯一开始与终结记录，无READ_EXCEPTION或DATA_ISSUE，4条READ_UNAVAILABLE分别是3次revision与1次绝对精度返回null。

重新统计bodyStateCoverage=1/1、modelSurfaceRecordCoverage=222/222、modelSurfaceRequiredFieldCoverage=222/222，无缺少面记录、无体列表独有记录、无截断。这里100%只针对本次枚举的第一步字段，不代表整个几何识别、有效点采样或等厚覆盖率。

native revision三次null，不符合当前required identity/metadata条件，因此identityStableWithinObservedFields=false和metadataComplete=false、outcome=PARTIAL是诚实结果。绝对精度null也应保持UNKNOWN，不能仅凭相对精度可读就断言绝对getter异常或有效精度模式已认证。effectiveAccuracyMode仍为UNVERIFIED_API_VALUES_ONLY。

不要用此前CREOSON revision=1冒充原生Model.GetRevision返回值；两者来源和语义不同。后续如需补充身份稳定性，可核实并另加GetVersionStamp等独立读取字段，保留原revision为null，并按新的明确规则审核，不静默修改旧日志或降低门槛让它变成成功。

## 与HTTP原始数据交叉核对

本次原生222个surfaceId与此前HTTP222个surface_id集合完全一致，所有同ID面积数值逐项相等。与历史原始文件的散列关联见audit.json。这加强数据一致性证据，但两次采集不是同一已认证事务，不能独立证明其间模型从未变化。

旧“任一轴extent跨度≤0.01”规则产生118个候选，在此样本中全部对应真实平面，但漏掉8个真实平面。原生真实平面数是126，不是118；该漏检结果仅针对本数据，不推广为所有模型无误报。后续按真实类型筛选平面，同时保留圆柱面用于折弯区域研究；不能只采平面再宣称全局等厚。

## 运行与身份边界

截图显示测试模型和Collection ended提示，与报告“命令运行成功”一致；该提示也明确要求检查summary。完成提示不能替代PARTIAL及UNKNOWN。日志显示dirty前后false、句柄相同，支持所观察字段未变化，但不独立证明任何模型写入均未发生；原日志modelWriteAbsenceIndependentlyProven=false应保留。

包内没有实际加载class/dat/资源散列，不能从日志格式独立认证运行文件与交付逐字节相同。截图不证明模型已保存或所有交互未发生。未保存作为操作者报告保留，不推定配置恢复状态。原始报告含本机证据目录，本公开审核只保留文件角色和散列，不重发路径或完整几何日志。

## 建议下一步

保留第一步源码与二进制，不因原生revision不可用而反复重做相同采集。下一轮将身份增强与几何采样作为明确改动：分别读取独立版本戳，保持revision未知；对平面及圆柱面设计有效域内点/法向采集，记录失败和覆盖率，再研究相对面距离与局部厚度。未完成采样前仍不确认薄壁、等厚、可展或转换可行。

本轮没有点、法向或距离数据，samplingCoverage=NOT_REQUESTED_STEP_1正确。不启动转换、不Save、不修改Web、SISWSheetMetalConvert或Ribbon持久化，不为本次审核生成新class/JAR。
