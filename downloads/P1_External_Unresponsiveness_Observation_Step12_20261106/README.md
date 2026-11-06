# P1 第十二步：仅补现场外部未响应观察

2026-11-06，Asia/Shanghai。本包仅记录表、清单、引用与校验；不含新代码/class/JAR，不修改采样、配置或现有汇总工具。现场仍用同模型200002100.prt、同Step10 REDUCED，连续运行3次。不保存模型、不点原生转换、不切换FULL/REDUCED，不重启/改部署来完成本记录。

三轮真实点击、消息返回及未响应观察仍待现场执行；云端未操作Creo。本包没有三轮结果，也不把第十一步旧记录补写成新观察。过去未记录的时间继续UNKNOWN。

1. 打开当前已有Step10 REDUCED，先确认模型与模式保持；只做原定三次命令，不夹入模型或其他插件操作。
2. 每次点击前准备记录时间，点击时记observed_command_start；消息区出现“Step 10 sampling ended...”时记observed_command_end。它们是外部观察时间，不能抄COMMAND_ENTER/EXIT或elapsedMs代填。
3. 全程观察Creo是否出现“未响应”：看见则YES，记首次看到/恢复的时间；全程观察但未看到则明确NO，暂停两列留空；漏看/不能判断则UNKNOWN并写备注，不能为了完成表格填NO。时刻按真实观察精度填写。
4. 一轮消息返回后，从该轮新业务日志名/内容抄实际request_id；再开始下一轮。别点Step9，别用旧UUID、改名或复制同一日志凑次数。
5. 填observations.csv。session_id沿用本次实际会话的稳定标记，三行仅在确为同会话时相同。model_context/configuration_id预填的是任务计划标签，不是已执行证明，也不会修改配置。request_id为空的行仅是计划；初始UNKNOWN也不是已完成观察。
6. 第3次返回后保留3份独立geometry-sampling-step10-UUID.jsonl和覆盖这3次的最终geometry-timing-step10-lifecycle-T10-*.jsonl；同一生命周期文件可涵盖3次，无须复制成3份。回传原件和填好的CSV。未响应的备注或已有辅助观察可一并保留，结束截图不能替代起止时间。

时间建议带时区ISO，例如2026-11-06T14:00:00+08:00（仅格式示例，禁止照抄为现场事实）。日期/时区和精度需真实，不能用文件修改时间补点击或暂停起止。notes可记漏看、多个暂停段、观察精度等。CSV兼容第十一步原工具，字段中文说明见现场记录说明.md。

复用已交付第十一步工具，不改代码。新收集的原件放input/raw，填好的CSV放input/observations.csv，在已有第十一步工具包目录运行：

```text
python tools/summarize_step10_runs.py --input input --observations input/observations.csv --out step12-result-new
```

本包没有tools目录；上条命令应在原第十一步工具包中执行，勿误当成新工具。结果目录必须是新目录，已有目录不覆盖。旧工具下载地址和不变身份见references.json。已有可运行工具无需重装或更改配置。

对照结果保留缺失/未知，不将记录齐备等同于根因已定位。消息可见时间和原生回调时间边界不同；消息在源码中先于最终摘要/close，不能要求二者完全一致。连续3次同REDUCED只是重复观察，不是FULL对照，不认证性能因果、暂停已修复、材料占据/壁厚或全过程零写入。
