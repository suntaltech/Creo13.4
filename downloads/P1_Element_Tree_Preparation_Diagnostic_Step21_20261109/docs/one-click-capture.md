# Step21 一次现场定位

1. 校验manifest/SHA256SUMS和实际加载散列，使用既有实际SDK/许可及Java25。短注册模板name=p1s21，填写绝对路径，不覆盖Step20注册/Ribbon。本包未部署，未自动运行。
2. 同目标当前普通零件200002100.prt、mm、初始dirty=false、唯一默认体非钣金；预选该体单个可见平面。当前实际枚举恰5特征且全ACTIVE，保留原体/面/模型版本门。不以此前ID或1.2mm代填，不为通过dirty门保存/清理模型。
3. 保留Step20和更早所有一次锁，不读取编辑/删除/重置或重试旧命令。只主动点击SISWElementTreeStep21一次，新锁在用户目录CreoElementTreeDiagnosticsStep21/step21-200002100-once.jsonl。取消/阻断也消耗，不重复点、换目录或重载绕过。
4. 第一次确认明确本步不创建/再生/保存；接受后输入明确正有限测试厚度，复检后第二次确认显示原面ID/本次厚度，并明确“只准备内存树”。接受后逐项准备一次，立即结束；没有写提交意图或模型创建/再生路径。
5. 若卡住，记录最后START的event/operation/probeId及elementIndex/elementId/level（如有）。SELECTION_BODY/FACE、ELEMENTS_CREATE、ELEMENT_VALUE_CREATE、ELEMENT_CREATE、ELEMENT_APPEND、SESSION_CREATE_ELEMENT_TREE可分别定位最后可见接口。不要仅凭外层ELEMENT_TREE_PREPARE_START判断具体调用。
6. 若正常完成，核对内部边界、SESSION_CREATE_ELEMENT_TREE_DONE.returnedNull=false、外层ELEMENT_TREE_PREPARE_DONE和ELEMENT_TREE_PREPARED_NO_MODEL_WRITE_NO_SAVE。若返回null或异常，依真实日志记录阻断，不补成功终态。一次完成不认证Step20卡顿偶发，不自动恢复转换。
7. 回收新JSONL原件、相关stderr/jlink、实际加载散列、界面/点击/人工输入/未响应起止时间。正常对话框等待不当作卡死。宿主不返回时保留已有证据，不再点击或补DONE；现场既有事故处理自行执行，本包不自动杀进程/重启/Undo/删除/恢复。

当前仅离线检查通过，真实元素树调用是否返回及卡点均NOT_EXECUTED/UNKNOWN。没有保存入口，也不拦截用户其他保存入口；不为补证据编辑或保存试验模型。
