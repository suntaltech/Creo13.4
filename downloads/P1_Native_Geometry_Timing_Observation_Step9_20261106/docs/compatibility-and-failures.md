# 兼容与失败分支

原P1_NATIVE_READONLY_GEOMETRY_V1及Step8全部基础/采样/质量/池/队列/空间/配对/distance/classification/interfaceReview schema和几何字段类型保留。collectorStep=STEP_9、独立命令/命名空间/业务文件前缀；每行增加timingSchema=P1_NATIVE_TIMING_OBSERVATION_V1和timingPhase。严格消费方需识别新增事件/字段，不能假设业务行数与Step8恒等。生命周期使用独立schema P1_NATIVE_GEOMETRY_TIMING_LIFECYCLE_V1，不混入旧几何事件计数。

1. 注册获取Session/创建/Designate失败：记录现有失败及对应动作跨度，保持原解绑/重启保护，不额外重试原生调用。start未被Java调用前的失败不可能由该类补日志。
2. 生命周期目录/CREATE_NEW创建失败：IOException或RuntimeException转stderr回退，错误类/计数可见，不阻断原业务。写入/序列化失败只发最小TIMING_WRITE_FAILED并累计错误；不会自动重开、无限重试或补造完整JSON。文件写入失败后后续仍尝试原writer，可能继续失败。回退stderr也不可用、进程被终止、内存耗尽等情况仍可能无证据；不承诺处理VM致命错误。
3. 业务文件创建或原业务写入/flush失败：保留原异常行为，外层标COLLECTION_OR_LOG_IO_FAILED。业务失败仍可使几何结果不完整；新增时序不是事务回滚/持久化保证。两个可选业务TIMING事件若写入失败，记录BUSINESS_TIMING_RECORD_FAILED继续原流程，但可能留下部分行，必须检查JSONL完整性。
4. read返回null/抛异常仍保留旧UNKNOWN/PARTIAL/READ_*，仅增加动作组聚合。原错误/身份/精度/INSIDE门禁不放宽；旧4项未知不被版本戳替代认证。
5. 早退或异常阶段标EARLY_TRY_EXIT/THREW_OR_ABORTED；Run.close结束残留阶段，记录close及总计；尚未构造成功的Run没有总计。进程冻结或被杀可能仅有PHASE_BEGIN而无END，这只能定位最后可见边界，不能自动证明内部哪次调用阻塞。
6. 停止时仍在采集：原busy保护不变；失败stop不关闭仍使用的日志。成功stop关闭生命周期文件，后续start新trace。关闭错误累计，但最后stop关闭之后没有新记录保证可读；不可据缺失错误断言零失败。
7. 原生仍在调用者线程。新增时序可能增加UI等待与30秒预算消耗；本轮是观测，不是性能优化，也不能从动作/输出总耗时直接证明Windows“未响应”窗口。

离线测试只覆盖纯计数、界限、负时长防护、线程ID标记、真实临时文件与创建失败回退；未模拟模型/SDK/native故障、真实磁盘写入失败或宿主UI。尚不能宣称实际几何结果保持、暂停已消失、材料占据、遮挡或全局等厚完成。
