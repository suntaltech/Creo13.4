# P1 默认体只读诊断第十四步候选

2026-11-06，Asia/Shanghai。独立类p1.readonly.body.step14.P1DefaultBodyDiagnosticStep14，命令SISWDefaultBodyDiagnosticStep14，显示名Default Body Read-Only Diagnostic Step 14。只针对当前打开的200002100.prt读取体信息，可多次运行；同一时刻busy保护，不消耗转换点击，不弹风险确认。

本步不调用WCreateFeature/WRegenerate/Save，不重试转换，不打开/读取/删除/改写Step13一次锁，不修改旧转换代码或判断。新日志单独写用户目录CreoDefaultBodyDiagnosticsStep14/default-body-step14-UUID.jsonl，CREATE_NEW及逐行flush，日志UUID每轮独立。当前没有部署/注册/运行Creo，也没有得出实际体状态或阻断根因。

记录活跃模型文件名/类型/单位、Model.ListItems(ITEM_BODY)数量、每个body的GetId、Java包装类型、getptr句柄及IsSheetmetal返回/异常。默认体分别从Step13的WFC视图和公开PFC Solid视图读取，不将一个视图结果替换成另一个；各自与枚举第一项分别输出Java同对象、句柄与ID比较。不会再因前一个条件成立而跳过后面的状态读取。

所有原生读取是这次命令内的新查询，不读旧Step10日志作为当前false。空对象、读取抛异常、接口不可用和真实false分开；枚举项不实现SolidBody时，明确JAVA_SOLIDBODY_INTERFACE_NOT_IMPLEMENTED、nativeCallAttempted=false，而不是伪造原生IsSheetmetal结果或强行包装未知体对象。

真实otk.jar/Java25编译无错误无警告；3份源码、6个class、薄JAR，两次干净编译散列一致，JAR与独立class字节一致。class major61但SDK major69，运行仍需Java25；SDK/JDK不随包，pfcasync不使用。60项纯身份比较断言通过，无SDK/模型对象或Mock。实际24项SDK前缀方法引用在允许清单内（含接口归属的Java getClass），没有模型写、风险确认、异步或转换锁调用；这些不是精确JNI次数。

包内有source/classes/lib、独立消息/未启用注册模板、manifest、SHA256SUMS、javap、静态调用表、测试和诊断口径。使用前读docs/probes-and-interpretation.md与docs/controlled-readonly-capture.md。当前Step14真机数据全部NOT_EXECUTED/UNKNOWN，不自动推断下一次转换、不恢复Step13试验资格，不认证材料占据/壁厚或生产可用。
