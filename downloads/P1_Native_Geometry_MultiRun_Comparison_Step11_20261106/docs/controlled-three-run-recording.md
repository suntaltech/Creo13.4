# 真机三轮记录清单

范围仅现有Step10同步只读采集，不保存/转换/再生模型，不改精度、INSIDE、采样配额/预算，不引入射线/材料/遮挡API，不后台化原生调用。本工具不执行这些现场操作。

1. 保留当前可运行Step10部署与同一打开模型，记录session_id、model_context、configuration_id（稳定的现场标签），实际Java/SDK/加载JAR或classes散列。用现有manifest核对，记录事实到session-context.json；声明相同不等于独立认证。模式保持原REDUCED；本任务不切换FULL、不改环境或重启配置。
2. 确认入口SISWReadOnlyGeometrySample10 / Sample Read-Only Geometry Step 10，避免误点Step9。按顺序执行第1、第2、第3次，每次等命令完整返回再开始下一次；不夹杂其他插件/模型操作。记录是否首次执行/此前误点、会话和模型是否变化。若变化或异常，不补造连续性，标明并保留证据；不自动重试或模型恢复。
3. 每次在observations.csv填写run_order与日志实际request_id；外部observed_command_start/end记录用户动作至返回观察的时刻，原生回调起止由JSONL独立提取，不把后者伪装成外部观察。所有时间用带时区ISO，例如2026-11-06T14:00:00.000+08:00，记录可达到的精度即可，不编造毫秒。
4. 未响应明确出现填YES并记录unresponsive_start/end；未观察到填NO，两个暂停时间留空；未观察/无法判断填UNKNOWN。YES但无法测出起止应留空并在notes解释，工具会保持未知/待补，不用0或命令总耗时代替。一次命令若出现多个间隔，将首末边界和所有分段记到notes或单独观察附件，首末跨度不是各停顿累计时长；不得当成单一暂停持续时长。
5. 每轮保留独立geometry-sampling-step10-UUID.jsonl。生命周期可能同一trace持续记录3次，只需在第3轮返回后复制最终完整geometry-timing-step10-lifecycle-T10-*.jsonl一次，包含所有请求；可同时保留其他完整trace文件。不要为了产生3个日志名而复制同一request。不要编辑原件、追加假时间或删掉失败记录。
6. 推荐回收目录input/raw存3份业务JSONL及最终生命周期文件，input/observations.csv、input/session-context.json及可用stderr；截图仅辅助，不提供暂停测量。原件含模型XYZ/UV和可能的机器路径，留在受控证据中，不自动发到公共仓库。工具只读取既有文件；允许复制最终原件供回收，不额外保存模型。
7. 日志完整后用工具输出新的结果目录；一轮未完整/退出码2仍保留已生成的报告及原因。回收至少3个独立完成请求、配对时序、外部观察与同上下文声明。UNKNOWN/PARTIAL保持，不能因为3次都相同就将revision/绝对精度未知变成已知，dirty=false不独立证明全过程零写入。

这三次是相同REDUCED条件下的重复观测，支持描述波动与暂停是否重复；它们不自动构成FULL/REDUCED因果对照，也不会确定阻塞根因或材料/壁厚。若后续需要其他实验，单独定义，不在本轮夹杂模式切换或线程/采样改动。
