# P1 原生几何日志开销第十步离线候选

2026-11-06（Asia/Shanghai）。独立类`p1.readonly.geometry.step10.P1ReadOnlyGeometrySamplerStep10`，命令`SISWReadOnlyGeometrySample10`，显示名`Sample Read-Only Geometry Step 10`。

本轮只优化业务JSONL的stderr镜像选择。默认REDUCED，保留摘要、异常/未知、身份与未来未知事件，将明列的高频几何事件与大时序快照只写完整业务文件；可用FULL模式对照。所有原业务JSONL事件、几何字段、顺序和逐条flush保持，不批量缓存，不移动原生调用到后台，不新增射线/材料/遮挡API，不保存/转换/再生或修改模型、精度、预算、INSIDE、Web/Ribbon和原命令。

第九步实测命令7907.743 ms，stderr累计1103.873 ms，flush 184.363 ms。本轮先隔离stderr选择，批量写盘只作方案评估。不能将这些历史时间直接当成本候选节省量或宣布暂停根因已解决。

实际otk.jar/Java25编译无错误无警告；14个源码、25个class，两次干净编译散列一致，薄JAR与独立class字节一致。仍是原58项PTC方法引用，原几何方法、read动作、完成门禁及30秒软预算冻结核对。class major61，但所用SDK major69，实际运行仍需Java25；不随包分发SDK/JDK，不用pfcasync。

原6638项纯断言继续通过，新增42项策略断言通过，共6680项（含性质检查，不是独立场景或真机次数）。另对Step9实际6432行做SDK无关的文件回放：FULL镜像6432、REDUCED镜像27，其余6405仅省stderr；两种模式业务JSON文本逐行原样全部保留，关键异常/未知/开始/结束记录保留。该回放没有执行采集器或SDK，不是Step10实际结果或性能基准。

交付含source/classes/lib、消息/注册模板、manifest、SHA256SUMS、javap、静态冻结、计数测试、派生回放和失败/对照说明。原始XYZ/UV日志、截图、SDK/JDK与现场路径不重新分发。Step10真机采样、耗时改善、暂停和现场加载身份仍NOT_EXECUTED/UNKNOWN，材料占据/遮挡/最终壁厚未认证。

先读docs/logging-policy-and-batching-evaluation.md与docs/controlled-comparison-and-failures.md。继承分类与SDK静态目录文档只描述已有语义，不授权新增接口试调用。
