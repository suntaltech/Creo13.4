# P1 原生几何时序观测第九步离线候选

2026-11-06（Asia/Shanghai）。独立类 `p1.readonly.geometry.step9.P1ReadOnlyGeometrySamplerStep9`，命令 `SISWReadOnlyGeometrySample9`，显示名称 `Sample Read-Only Geometry Step 9`。目标是为Step8加载/命令短时未响应补充观测，尚未定位原因，也不宣称修复卡顿。

本轮只增加同步时序、聚合计数和独立生命周期JSONL。实际58项PTC方法引用与Step8完全一致；原采样、解析、双INSIDE、空间改善、局部距离和分类方法块冻结，精度/非共线/30秒软预算不改。不移原生API到后台线程，不增加射线、材料占据或遮挡调用，不保存/转换/再生模型，不改Web、Ribbon和原业务入口。

输出保留业务 `geometry-sampling-step9-UUID.jsonl`，新增 `geometry-timing-step9-lifecycle-T9-*.jsonl`，均CREATE_NEW。目录仍用 `P1_READONLY_GEOMETRY_LOG_DIR`，未设置则用户目录下CreoReadOnlyGeometry。生命周期文件在Java静态初始化时创建，业务文件在命令回调内创建；必须保留两类原件。traceId与requestId用于关联，不根据文件名或跨进程nanoTime拼时间线。

真实otk.jar/Java25编译通过、无错误无警告；13个源码/24个class，重复干净编译散列相同，薄JAR与class字节相同。class major61，但SDK依赖major69，实际运行仍需Java25。SDK/JDK不随包，pfcasync不使用。原6612项纯断言继续通过，另26项日志计数/实际临时文件/回退断言通过；这些不是6638次真机试验。云端未注册或运行Creo，无模型Mock/SDK Stub。

交付含源码、class/JAR、消息与注册模板、manifest、SHA256SUMS、javap、原调用表和新时序说明。原始模型XYZ/UV日志和截图不重新分发。Step9实际注册、时序、暂停原因和原采样保持情况全部NOT_EXECUTED/UNKNOWN。

请先读docs/timing-events-and-counting.md、docs/compatibility-and-failures.md及docs/controlled-verification.md。继承的classification-rules与sdk-interface-review文档描述Step8已有语义，不是Step9新增接口调用。材料占据、遮挡、全局壁厚、可转和独立零写入认证均未建立。
