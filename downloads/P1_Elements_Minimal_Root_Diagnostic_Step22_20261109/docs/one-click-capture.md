# Step22 一次现场最小定位

1. 校验manifest/SHA256SUMS和实际加载class或JAR散列，仍需既有SDK/许可与Java25。registration/step22.dat.template短名p1s22，填写绝对路径；不覆盖Step21注册/Ribbon。本包未自动部署。
2. 保留Step21预检条件：当前普通零件200002100.prt、mm、dirty=false、唯一默认体非钣金、原体单个可见平面预选、实际恰5特征全ACTIVE。不以旧ID/版本或1.2mm代填，不为通过dirty门保存/清理模型。
3. 不读取编辑/删除/重置Step21及此前锁。只主动点击SISWElementsMinimalStep22一次，新锁/日志在用户目录CreoElementsMinimalStep22/step22-200002100-once.jsonl；成功/取消/阻断都消耗，不重复点击、换目录或重载绕过。
4. 两次确认默认NO，期间明确输入正有限测试厚度。厚度仅日志记录，不应用到根或任何其他元素；第二确认说明仅Elements.create、根构造/追加一次。完成确认/复检后开始最小探针。
5. 如卡住，回收最后event/operation/probeId以及根elementId/level/index（如有）。区分MINIMAL_CREATE、ROOT_VALUE（无SDK工厂）、ROOT_CREATE、ROOT_APPEND，不再根据外层START猜测后续调用。不要为补DONE重复点。
6. 如完成，核对ELEMENTS_CREATE_MINIMAL_DONE.returnedNull=false、ROOT_ELEMENT_CREATE_DONE.returnedNull=false、ROOT_ELEMENT_APPEND_DONE及MINIMAL_ROOT_APPENDED_NO_MODEL_WRITE_NO_SAVE；无其他元素、SESSION_CREATE_ELEMENT_TREE或写模型事件。null返回/异常就如实记录阻断，不补成功结果。
7. 回收本轮JSONL原件、相关stderr/jlink、实际加载散列及外部点击/确认/未响应起止时间。正常模态等待不当作卡死。宿主失效保留证据，按现场已有事故流程处理，本包不自动杀进程/重启/Undo/删除/恢复模型。

本轮成功不认证旧卡顿偶发；若仍卡在Elements.create或根构造/追加，仅据真实证据选择下一轮实现，不在本轮自动尝试数组/构造替代或恢复转换。当前云端未执行模型/SDK探针，真机结果NOT_EXECUTED/UNKNOWN；不编辑或保存试验模型以补证据。
