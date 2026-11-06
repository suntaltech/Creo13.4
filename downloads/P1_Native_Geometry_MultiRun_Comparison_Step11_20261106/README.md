# P1 第十一步多轮对比准备与汇总工具

2026-11-06（Asia/Shanghai）。本轮复用现有Step10，不发布新算法、class/JAR或新命令。目标是在同模型、同配置、同会话连续运行Step10至少3次，回收独立requestId的完整业务/生命周期JSONL、每次外部命令起止及未响应起止，统一比较elapsed/read/output/stderr/flush与阶段。

云端不能执行现场Creo。现有仅一轮Step10真机材料，不将它、复制文件或纯回放当成三轮受控结果。本包是可执行的记录/汇总准备；三轮真机与完整外部观察仍待回收，不声称性能收益、暂停消失、材料占据或壁厚。

使用现有Step10候选，代码与二进制不变。预计薄JAR SHA256：b20652ae84558b306618da9d590b6fcadaa15c5be1efbe441f645f35bbb35010。SDK/JDK不随本包；实际加载classes时按Step10原manifest/SHA256SUMS核对全部25个class，不能只核对单个主类。引用身份见baseline-identity.json，实际现场加载身份仍需记录。

步骤见docs/controlled-three-run-recording.md与docs/analyzer-and-limits.md。templates/observations.csv提供3行空记录；request_id留空的计划行不会计作已运行。templates/session-context.json记录会话和加载事实，UNKNOWN需如实保留。工具只分析文件并写入一个新的结果目录，不调用SDK/模型/GUI/网络，不自动点命令、改环境、重启或保存模型。

需要Python 3.9或以上（标准库，无pip依赖）。将至少3轮原始日志放入input目录并填写观察CSV，然后运行：

```text
python tools/summarize_step10_runs.py --input input --observations templates/observations.csv --out result-new
```

输出comparison.json、runs.csv、phases.csv和README.md。退出码0表示记录齐备且申报上下文相同；2表示缺少运行/观察或存在不一致（仍输出报告）；1表示输入/输出使用错误。已有结果目录拒绝覆盖。

证据目录evidence只有历史单轮Step10的工具验证摘要与测试记录，不是新三轮数据。测试需现有原Step10证据目录作为只读输入：

```text
python tests/test_comparison.py path-to-original-step10-evidence
```

回传三轮原件、观察CSV、会话事实及汇总结果；不要只回传“ended”截图或一份摘要，不修改原JSONL补值。
