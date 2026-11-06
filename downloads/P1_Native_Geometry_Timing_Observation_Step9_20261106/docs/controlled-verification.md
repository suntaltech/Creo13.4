# 受控真机回收清单

本包仅离线候选；云端未部署/注册/运行Creo。仍遵守既有受控现场范围，不扩展为保存/转换/再生/改精度/新射线或材料调用。

1. 核对ZIP SHA、manifest、SHA256SUMS、实际SDK和Java25；只用独立Step9目录/类/消息/命令。注册模板路径需现场填写；不可直接覆盖旧注册、原Ribbon或业务入口。class路径模板默认独立classes；如改用薄JAR也要记录加载散列，避免混用旧class。
2. 从开始加载前记录操作者UTC及动作：加载插件、命令进入、短时未响应是否出现、何时恢复。分别区分加载和点击命令，不只回收结束截图。没有外部加载前时间只能观测Java可见边界，不能补造完整类加载耗时。
3. 保留新geometry-timing-step9-lifecycle-T9-*.jsonl和geometry-sampling-step9-UUID.jsonl以及可用原stderr。生命周期可只有一份而多个命令request；按traceId/requestId关联，不修改/裁剪原文件。生命周期失败时收集[P1GEO_TIMING] stderr及错误，未采集stderr需写UNKNOWN。
4. 检查静态初始化/start注册动作与COMMAND_ENTER/EXIT；若卡在加载前且没有首条事件，保留“Java入口前未知”，不要推为几何采集卡顿。按PHASE_BEGIN/END、计数和输出ns找占比；阶段含Java与日志，不当作纯原生API性能排行。
5. 比较快照与生命周期完整业务总量，核对COLLECTION_SUMMARY最后一条、close完成、输出失败/丢失行、overflow和负时长标记。自写排除和阶段边界间开销不能靠强行配平消除。
6. 同时核对Step8原几何基线与UNKNOWN/PARTIAL。参考此前222面、666点/2702尝试、32候选/196见证、双INSIDE拒绝76、126组非共线等；模型/时间预算不同可能不同，不能以模板数字补值。原4项未知、严格revision和身份判定继续保留，不把dirty=false升级成独立零写入证明。
7. 若未响应持续或出现异常，按现场受控流程停止后续操作，保留原证据。不新增自动重试、Save、Undo、模型恢复、后台原生调用或新SDK试探。回收后先审时序及原几何差异，再制定后续性能任务。

Step8历史观察：COLLECTION_SUMMARY elapsed7732ms；业务起止UTC span7706.943ms，分类START→SUMMARY24.062ms，静态SDK目录START→SUMMARY3.988ms。这些只是旧日志边界，排除加载前/部分准备与UI重绘，不是本轮测量，也不能证明暂停原因或持续时长。
