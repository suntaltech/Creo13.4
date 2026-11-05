# 只读分析 JSONL 审核

日期：2026-11-05。审核用户上传readonly-analysis-log-20261105.zip。本次只解析材料、生成审核记录，未运行HTTP、浏览器或Creo，未更改代码。

## 结论

本次补齐了一轮完整的分析审计链，与此前纠偏截图相符：一个实际分析requestId、5次逻辑读取、一个成功结果摘要。可以冻结本轮“取消误导分类并提供分析审计”的实现，保留现有Web和导航二进制，不再为同一目标生成新候选。

JSONL共13条：入口guard、分析开始、5对读取开始/返回、结果摘要。真实requestId=smt-analysis-1793859047495-1，直接提取自上传文件。UTC06:10:47.495至06:10:49.530，北京时间14:10:47.495至14:10:49.530，耗时2.035秒。这里是这一轮日志时间差，不是普遍性能指标。

## 调用与摘要核对

逻辑顺序为file/get_fileinfo → geometry/get_surfaces → geometry/bound_box → file/get_length_units → file/get_fileinfo，四种唯一读取接口、五次逻辑读取。每次均有同requestId的started/passed，未出现失败或转换事件。

逻辑读取事件不包含data、响应体或网络重试次数；共享core可能建立连接/重试读取，所以不能把五次逻辑读取宣称为恰好五次HTTP请求，也不能据导出中无连接事件认定没有建立连接。

摘要model=200002100.prt、revision=1、units=mm、surfaceCount=222；revisionCheck=FILE_AND_REVISION_MATCH，unitReadOutcome=RETURNED_UNIT_STRING。与前次页面显示相符。原生状态UNKNOWN、薄壁/等厚INSUFFICIENT_EVIDENCE、estimatedThickness/suggestedMode/planeCount均null，nativeConvertReady=false；未再把特征历史缺失写成块状实体或默认推荐抽壳。

日志中的saved均false，未发现sessionId/token/authorization类字段。检查仅针对提供的导出文件，不认证原始共享事件是否含凭据。导出是前端生成的摘要，不能独立证明服务器调用、模型从未修改、单位语义正确或实际加载文件身份；前后fileinfo原始值并未随包提供。

## 对下一阶段的意义

这份JSONL是审计摘要，不是曲面几何证据。它没有surflist条目、曲面类型、法向、相对面距离或厚度样本，无法独立验证等厚、薄壁形态或可展性。字段null/证据不足是当前正确结论，不应为了匹配已知样本而改成“已识别”。

下一步优先提供既有读取的脱敏原始请求与响应：两次file/get_fileinfo、geometry/get_surfaces、geometry/bound_box和file/get_length_units。保留command/function/data结构、单位、revision和几何字段，去除sessionId/token/authorization等凭据；不要凭截图重构响应。如果现有原件未留存，应记录缺失，任何重新采集仍按相应真机授权范围进行，不点击转换或保存。

先判断真实get_surfaces响应到底包含哪些属性；若只有面积和extent，就明确不足，再依据真实SDK签名设计只读几何采集。不得虚构API、把轴向extent当法向距离、用包围尺寸代替壁厚或在没有覆盖率证据时确认全局等厚。

本轮不新增采集代码、不重编Java、不部署、不修改模型/配置，不重新发布原始JSONL；audit.json保留原件散列、逐读取原行号和解析结果供整合。
