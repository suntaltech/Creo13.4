# Step23 一次现场根元素定位

1. 核对manifest/SHA256SUMS、实际加载散列及SDK/JDK；仍需Java25。先阅读evidence/sdk-overload-assessment.json：当前离线SDK只支持Elements参数，本包没有可执行数组重载。若现场SDK不同，不能直接套用此签名结论。本包未部署。
2. 短注册模板name=p1s23，填写绝对路径，不覆盖Step22注册/Ribbon。保留当前普通零件200002100.prt、mm、dirty=false、唯一非钣金默认体、该体单个可见平面预选、实际5特征全ACTIVE及版本门；不以旧值代填，不为通过dirty门保存/清理模型。
3. 不读取编辑/删除/重置Step22或更早锁。仅主动点击SISWRootElementProbeStep23一次，新锁/日志位于用户目录CreoRootElementProbeStep23/step23-200002100-once.jsonl；取消/阻断也消耗，不重复点击、换目录或重载绕过。
4. 保留两次默认NO确认和明确正有限测试厚度输入。厚度仅记录，根value始终null、level始终0。最终确认说明只探测根，不尝试不存在的数组/集合重载，不使用原Elements工厂或append、不写模型。
5. 若卡住，记录最后ROOT_ELEMENT_CREATE_START的operation/probeId和elementId/level，回收stderr/jlink。不要为确认是否必挂重试。若有DONE且returnedNull=true，如实记录阻断，不写成根构造成功。
6. 根非空返回后，应有TREE_ALTERNATIVE_UNAVAILABLE及ROOT_RETURNED_NO_TREE_OVERLOAD_NO_MODEL_WRITE_NO_SAVE，明确treeConstructed=false；这不是单根元素树成功。没有数组重载START/DONE是设计事实，不能要求补齐或伪造。
7. 回收本轮JSONL原件、相关stderr/jlink、实际加载散列、外部点击/确认/未响应起止。正常模态等待不当作卡死。宿主不返回保留已有记录，不补DONE；现场既有事故流程自行执行，本包不自动杀进程/重启/Undo/删除/恢复。

当前SDK签名已离线核对；真实根构造及稳定性NOT_EXECUTED/UNKNOWN。后续XML/其他PFC接口评估、完整7元素构造和恢复转换均需另行任务，不在本轮自动执行。
