# 分析当前零件：未来只读受控验证计划

本文件是准备清单，不是本机GUI执行授权。本轮不点击分析、不连接CREOSON、不运行HTTP/Java/浏览器测试，不做转换和保存。使用现有R8 classes即可准备该项；源码诊断候选尚未生效不妨碍阅读本计划，但日志格式须按实际二进制版本区分。

## 先核对实际实现，满足后才进入点击

实际sheetmetal.html、sheetmetal.js、core.js及其实际依赖/服务配置的相对名称、散列、加载来源必须先登记。reference-only-web来自仓库历史第五轮离线兼容补丁；提供其原字节和散列只为解释已有审计契约，不认证当前9056页面就是该实现，也不要求覆盖线上文件。此前截图的控件状态与该历史HTML有差异。若实际文件不同，先对比分析事件、网络调用及按钮禁用逻辑；未确认实际调用集合只读时，本计划停在身份核对，不执行点击。

确认实际分析路径未调用window.external或原生转换桥、没有WCreateFeature/WRegenerate/Save，也没有文件/特征修改、删除、重命名、模型单位变更、再生、导出或模型自动保存。以实际实现和请求记录核对，不以按钮名称“分析”或saved:false自证只读。转换控件与保存控件应处于禁止执行状态，不启用或编辑它们。只用既有测试普通零件，不换入生产模型。

## 观察来源和关联

J-Link的页面回调requestId仅标识打开页面，不是Web分析请求ID。参考实现为一次分析创建smt-analysis-... ID（只有真实点击才生成）；通过操作者记录把两个ID及同一测试模型、页面来源、时间范围关联，不将其混写。

参考core.audit把事件放在浏览器内存SiswCore.events，并非自动写入jlink.log、控制台或服务器文件。分析页没有现成“下载分析日志”按钮。未来经授权可只读导出/复制events，或使用既有SiswCore.downloadLog接口导出本地NDJSON（这会写本地证据文件，不是保存Creo模型）；不得启用调试方式去触发业务函数。刷新页面会丢失当前内存事件，先留存再关闭。清除CREOSON会话令牌、authorization等凭据，不把它们发布。

## 参考实现的预期审计字段（不是运行证据）

| event | 字段与意义 |
|---|---|
| smt_mode_guard | blocked、nativeConvertReady=false、saved=false；入口状态，不一定带分析requestId |
| smt_analysis_started | requestId、saved=false；一次分析开始 |
| smt_read_started / passed / failed | 同分析requestId、command、function、saved=false；每个逻辑读取调用，非逐HTTP尝试计数 |
| connected / connect_attempt_failed / connect_exhausted | 连接辅助事件；可能没有分析requestId，应按同一浏览器上下文/时间关联 |
| smt_units_unconfirmed | requestId、errorCode=UNITS_UNCONFIRMED、saved=false；单位不可确认不改成mm，也不自动阻止其他只读数据显示 |
| smt_analyze_http | requestId、units、nativeConvertReady=false、saved=false、model、sheetmetal、surfaceCount、planeCount；成功结果摘要 |
| smt_analyze_failed | requestId、errorCode、saved=false；失败显示，不应产生成功摘要 |

同一分析的逻辑读取顺序为file/get_fileinfo、feature/list、geometry/get_surfaces、geometry/bound_box、file/get_length_units、再次file/get_fileinfo。features/surfaces/bounds/units均针对最初返回模型。最后核对file及revision；若变化按MODEL_CHANGED失败。单位查询可失败后继续并记录UNITS_UNCONFIRMED；其他读取失败通常进入失败显示。参考file/revision前后相等也不能排除中途切换后又切回，操作者须避免并发操作。

连接准备允许connection/connect，用于会话建立，不等于模型写操作。参考core在不确定通信错误时会重连并重放一次读取，ensureSession还可能有启动连接重试；单条smt_read_started/passed不等于一个网络请求。保留完整原始网络/服务请求证据，不能按事件数伪造调用次数。HTTP POST用于读取也不代表模型写，须看实际command/function/data。

## 预期页面反馈

开始显示“正在通过CREOSON读取当前零件几何…”，分析按钮忙碌期间禁用。成功显示模型、revision、实际长度单位、曲面数、启发式包络候选数、候选面积、包围尺寸、最小包络跨度等；这些只是读取和启发式建议。

不能把planeCount当官方SurfaceType平面数，不能把largestPlane当真实选中面，不能把estimatedThickness当正式壁厚。参考sheetmetal=null、suggestedMode=null，未通过原生IsSheetmetal或实体数量检查。单位缺失须显示未确认，不假定mm或静默换算。无候选可显示“分析完成，未找到包络启发式候选”，这不是官方无平面结论。转换按钮仍禁用。

错误码可含MODEL_CHANGED、INVALID_RESPONSE、NOT_PART、TIMEOUT、NETWORK_ERROR、CREOSON_ERROR、ANALYSIS_FAILED，页面保留对应失败提示；不能补填成功或为凑数据继续点转换。阻塞/busy时未进入分析的点击可能没有started事件，应记录阻塞情况而非伪造ID。

## 写模型越界与停止条件

只允许上述已核对的读接口及必要连接。任何未列明/语义不明的请求先停；明确涉及create、modify、delete、rename、save、regenerate、convert、导出、更改单位或原生WCreateFeature/WRegenerate/Save时立即停止并隔离该次记录。不点击分析以外业务按钮，不修改选择或参数去推动转换，不操作Ribbon持久化，不手动恢复模型来掩盖变化。

保存日志/截图、内存事件、浏览器本地选项或建立会话可能有非模型副作用，必须与Creo模型写入区别记录；不得说“分析完全不写任何内容”。若观察到模型dirty/特征树变化或禁止调用，记录真实现象并停，不自行Save、Undo、删除或重试。只有实际检查覆盖的范围才能判定只读；saved:false、没有调用日志、file/revision未变均不足以单独证明没有模型修改。

受控记录应包含实际源文件/类/资源/Web身份、测试模型前后状态、审计事件原件及散列、网络读取证据、页面前后截图、错误与越界观察；无相关监测时填未观察/未确认，不把mustNotTouch计划填成实际未触及。完成后本机配置恢复仍按原授权及证据单独处理。本轮不生成已填记录、截图、日志或requestId。
