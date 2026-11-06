# 埋点、时间和计数口径

## 生命周期独立文件

| 事件 | 范围 |
|---|---|
| JAVA_STATIC_INITIALIZATION_OBSERVED | 主类最早可见静态字段到静态块末端，含计时日志创建，排除首条记录序列化/写入；JVM此前定义、验证、链接、宿主加载等待为UNKNOWN |
| PLUGIN_START_ENTER/EXIT | start回调内可见时间；不包含宿主调用前等待，EXIT自身写入排除 |
| REGISTRATION_ACTION_SPAN | 现有获取同步Session、Listener构造、UICreateCommand、Designate、失败清理/stop解绑，每次一条动作组 |
| COMMAND_ENTER/REJECTED/EXIT | 回调进入、inactive/busy拒绝、退出；EXIT在关闭业务文件后，排除EXIT自身写入及随后的UI重绘 |
| REQUEST_ID_GENERATED | 原有UUID生成耗时；未新增随机数初始化 |
| RUN_LOG_IO_BEGIN/END | 原目录创建、业务CREATE_NEW打开和EVIDENCE_FILE诊断，包含BEGIN埋点写入开销 |
| PHASE_BEGIN/END | 下表阶段；BEGIN先写，END记录起止、耗时、业务计数增量；异常/早退标明outcome |
| RUN_LOG_CLOSE | 业务writer.close耗时与是否返回 |
| RUN_OBSERVATION_TOTALS | 业务计数截至close，含最后COLLECTION_SUMMARY；生命周期自身本条和后续写入不计入当前state |
| PLUGIN_STOP_ENTER/EXIT | 停止与解绑；成功后关闭生命周期writer，下次start重新开新trace文件 |

每条生命周期记录带UTC timestamp、System.nanoTime、线程ID/名、traceId、sequence、requestId。尚无命令ID时requestId=NO_COMMAND_REQUEST_YET。UTC可受校时影响，耗时按同JVM单调时钟差；mono原点任意，不跨进程比较。静态初始化开始字段本身之前加载Instant等依赖的不可见部分不能当成完整类加载时间。

## 阶段

COLLECTION_SETUP_AND_STARTED_MESSAGE；ACTIVE_MODEL_IDENTITY_UNITS_ACCURACY；DEFAULT_BODY_STATE；MODEL_SURFACE_ENUMERATION；BODY_ENUMERATION_AND_FALLBACK；SURFACE_TYPE_AREA_AND_UV_METADATA；POINT_NORMAL_SAMPLING_AND_SPREAD；COARSE_PAIR_ENUMERATION；ANALYTIC_DESCRIPTOR_READS；DOMAIN_VALIDATION_AND_LOCAL_DISTANCES；CANDIDATE_CLASSIFICATION；STATIC_SDK_REVIEW_OUTPUT；FINAL_MODEL_IDENTITY；FINAL_SUMMARIES_AND_FINISHED_MESSAGE；FINAL_COLLECTION_SUMMARY_OUTPUT。末阶段在Run.close时结束。

阶段耗时包括该阶段开始日志与Java/SDK/输出等等待，不是独占CPU或纯JNI时间；结束记录写入不计入该阶段。不同层级耗时有包含关系，不能把全部指标直接相加。计时本身也有开销；原30秒预算继续生效并包含运行中新增开销，因此不能承诺新旧样本数和耗时恒等。

## 调用与输出

readActionGroups只统计原read包装器动作次数，区分返回null/抛出异常/正常，含每组total/max ns。动作可包含多个getter，直接enum.getValue/getptr等不单独包装，不能称精确SDK/JNI次数。动作内部耗时不包括之后READ_EXCEPTION/READ_UNAVAILABLE诊断输出。注册另计每次actionGroups=1。线程字段与differentReadThreadObserved只是观测，不认证SDK线程契约；不创建线程或异步原生任务。

按阶段+规范化操作名（方括号ID替换成[]）聚合，最多256个常规key及1个overflow桶；超额次数保留在桶内，不输出逐读取JSON。phaseReports最多32项。输出慢样本阈值10ms，最多保留最先16条达到阈值的事件，并非所有慢事件或最慢16条。

业务输出统计每次尝试/完成/失败、header+JSON序列化ns、writer.write/newLine调用次数及合计ns、flush次数/ns、stderr.println次数/ns、分类旁路观察ns、总输出ns及首末UTC。writerWriteNs包括write与newLine；分类观察单列；未改变原write→newline→flush→stderr→observer顺序。flush代表Java writer.flush，不保证磁盘fsync或持久化；PrintStream可吞I/O错误，println完成不等于stderr已被采集或显示。

业务TIMING_COMMAND_CONTEXT紧接原COLLECTION_STARTED；TIMING_OBSERVATION_SNAPSHOT位于原COLLECTION_SUMMARY之前。快照计数排除它自身、最后摘要及close，完整业务总量见生命周期RUN_OBSERVATION_TOTALS。业务COLLECTION_SUMMARY仍最后一条业务事件；它增加trace、观测口径与摘要之前日志错误数。生命周期state字段明确排除正在写的本条，无法无限递归记录自身写入。
