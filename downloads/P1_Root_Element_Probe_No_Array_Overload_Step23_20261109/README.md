# P1 元素构造替代第二十三步：根元素探针与SDK重载核对

2026-11-09，Asia/Shanghai。应用类p1.controlled.step23.P1RootElementProbeStep23，命令SISWRootElementProbeStep23，短dat name=p1s23。实际SDK无任务所期望的数组/非Elements集合建树重载，因此本包只交付独立根构造探针，不伪造不可编译的数组API，不宣称已构造单根元素树。

## 离线SDK核对结论

实际otk.jar的WSession.CreateElementTree唯一公开签名为CreateElementTree(com.ptc.wfc.wfcElementTree.Elements)。递归检查WSession全部父接口，同名方法没有Element[]或其他集合参数重载。Elements无公开数组构造器，唯一可见构造器为protected Elements(long)；这是句柄构造器，不是数组接口。本轮不反射调用该构造器、不伪造原生句柄，不用它绕过已报告卡顿。

WSession另有CreateElementTreeFromXML(String)，是不同方法，不是CreateElementTree的数组/集合重载；本轮没有XML构造说明或文件，不扩展探针到该方法。完整javap层级、构造器输出及机器可读结论在evidence/sdk-overload-*和sdk-elements-constructors.javap.txt，均来自真实SDK静态声明，不是Mock或运行时模型调用。结论绑定包内SDK SHA256，不冒称检查过现场实际装载的另一份SDK。

## 执行范围

保留Step22预检、两次默认NO确认、正有限厚度输入和确认间复检：当前普通零件200002100.prt、mm、初始dirty=false、稳定默认体/原捕获驱动面、实际恰5特征全ACTIVE、版本/dirty不变。fiveFailureFlagsChecked=false持续明确。厚度仅日志记录，不应用到根；不硬编码旧输入或ID。

完成体/面CreateModelItemSelection后，只调用一次wfcElementTree.Element_Create(PRO_E_FEATURE_TREE,null,0)。ROOT_ELEMENT_CREATE_START先写盘并force(true)，返回后ROOT_ELEMENT_CREATE_DONE只记录returnedNull，不追加原生getter。上下文为真实elementId、elementIdName、elementIndex=0、level=0、argumentKind=NULL_ROOT，不记录对象地址/树内容。

根非空返回后，纯Java日志TREE_ALTERNATIVE_UNAVAILABLE记录离线SDK证据来源、SDK散列、elementArrayOverloadAvailable=false、nonElementsCollectionOverloadAvailable=false、treeConstructed=false、fallbackAttempted=false，并立即结束。根返回null时DONE如实记录null，然后阻断，不假装取得根或建树。异常保留原class/message/operation和元素上下文；宿主不返回可能没有THREW/FINAL_RESULT，不补写DONE。

完全移除Elements.create及append，不构造Elements对象，不调用WSession.CreateElementTree或XML方法；无参数值工厂、其他业务元素、模型创建/再生/保存、自动重试或恢复转换。没有数组重载START/DONE，因为SDK不存在该调用。原ELEMENT_TREE_PREPARE_START/DONE仅作外层兼容事件，operation=PROBE_ROOT_ONLY_NO_AVAILABLE_TREE_OVERLOAD，DONE明确elementsAppended=0、sessionElementTreeCreated=false、alternativeOverloadAvailable=false。终态ROOT_RETURNED_NO_TREE_OVERLOAD_NO_MODEL_WRITE_NO_SAVE表示根调用返回但建树路线不可用，不是完整任务建树成功。

用户目录CreoRootElementProbeStep23/step23-200002100-once.jsonl为独立日志兼一次锁。CREATE_NEW；取消、阻断、失败、根返回均不删锁，不访问Step22及更早锁。同步主动命令、静态防重入/本进程一次点击，不因重载/换目录绕过，不自动杀进程、回滚或恢复模型。

## 验证与限制

真实SDK/Java25无警告编译，7源码/16class/薄JAR，class major61但SDK major69，仍需Java25。重复干净编译散列一致、JAR与散装class字节一致。52项SDK前缀方法引用，较Step22无新增；Elements创建/追加、会话建树/XML、模型创建/显式再生/保存及禁用状态flag引用为零，根Element_Create仅1处。109项SDK-free纯断言通过，验证审计/一次控制、体面门、严格PFC状态、二次确认和元素上下文；未执行根API、Creo或GUI，无模型Mock。

任务单报告Step22 requestId=13e3cc7e-37cf-4e6b-a09c-072f402f4af4：体/面选择DONE，最后ELEMENTS_CREATE_MINIMAL_START无DONE，未到根或append。本次未附原始JSONL/stderr，以上为用户报告，不独立宣称Elements.create在所有模型必挂。

根若卡住，仅回收最后ROOT_ELEMENT_CREATE_START及相关stderr/jlink，下一步再评估其他特征API；根若返回，也不能推断数组建树会成功或此前卡顿偶发。不自动补完整7元素树，不恢复转换。本包真实探针结果NOT_EXECUTED/UNKNOWN。

包含源码/class/JAR、消息/短名注册模板、manifest/SHA256SUMS、SDK签名核对、静态调用表、源补丁和纯测试证据，不含SDK/JDK、模型、现场原始日志/截图。先读docs/audit-boundaries.md和docs/one-click-capture.md。
