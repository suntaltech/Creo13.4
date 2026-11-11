# Step41 R7 复核：排除重复命名试验，转向命令/UI契约

日期：2026-11-11（北京时间）。本包仅包含离线复核、事件汇总工具修正和下一步最小SDK输入清单，不包含R8 DLL或插件补丁。

## 1. 本轮交付

- evidence/recomputed-runtime.json：从原始JSONL最后一次初始化重新计算。
- evidence/review.json：输入校验、对照结论、汇总冲突与加载方式变化。
- tools/review_events.py：仅按精确事件名计算回调标志，供本机助手后续整合；Python标准库，无SDK调用。
- next-input.template.json：下一轮只读契约检查范围。

外部SHA256与内部17项清单通过。上传源码逐字节匹配已交付R7，SHA256为8ecf88e80d366d7053a2c14475dff4393bd6b1fc1770f2f6c6d96fccb6e3faf9。没有修改原始输入文件。

工具使用：python review_events.py --log <已有JSONL> --output <尚不存在的报告文件>。只写新报告，不覆盖原报告、不修改原日志，无须重新采集或运行Creo。输入仅为日志，UI可见性仍须引用独立截图，不能从消息返回码推导。

## 2. 结论与必要纠正

最新pid=34372/load_token=27593281：六项注册/指定返回0、三个ID不同；面计数label/help与ModelInfo label解析0，面计数description与可见ModelInfo description均-32且截断到80；Ping description解析0、73宽单元。截图仍只有两个旧命令。

同一R7观察中，可见ModelInfo也处于相同描述截断状态，因此“描述截断必然导致隐藏”不是充分解释。不要据此直接缩短body描述来宣称修复，也不要扩大成截断在所有命令/上下文都不可能有影响。

当前runtime-review.json将body_callback_ENTER_observed写为true，但callback_events_observed为空、acceptance字段为false，文字结论也说未触发。按原始event精确匹配，实际没有BODY_FACE_COUNT_ENTER或RETURN。REGISTER_BEGIN不是回调进入。本包已提供派生纠正结果和正负例验证工具，不要求补做点击来消除数据矛盾。

交接建议的“action/command名称不一致”试验不应执行：

- facts.command是本工程日志功能标识，SDK没有接收这个JSON字段。
- SDK动作名当前为p1_readonly_fc；显示文字由资源label解析得到，需与日志字段区分。
- R3以前实际SDK动作名就是p1_readonly_body_face_count，当时已不可见。将其改回不能当成未做过的新对照。
- 不用“内部命令名”描述日志字段，不根据日志命名去改SDK注册身份。若发现官方确有动作名/显示标签关系要求，必须引用该关系的实际文档；目前没有这类证据。

## 3. 新的范围与下一步最小输入

标签/帮助在探针阶段确实可解析，继续泛称“文件或键完全找不到”已无依据；但解析发生在指定之后，不证明Designate使用同一路径。当前更应核查SDK注册对象与UI定制上下文的关系，不继续修改资源。

请本机助手只在已安装同版SDK/帮助中离线补两组最小片段，不编译、不启动/停止Creo、不改配置或资源：

1. 查找命令名到ID的只读查询接口，候选检索词ProCmdCmdIdFind。这只是待核实名称，不保证同版存在。若存在，提供完整声明、参数/返回码注释、允许的初始化/回调上下文，以及按名称查询得到的ID与ActionAdd返回ID的关系。若不存在，明确记录检索范围；不要猜一个替代原型。uiCmdCmdId=void*已知，无须重采。
2. 提供uiCmdAccessFn回调真实typedef、uiCmdAccessMode和uiCmdAccessState枚举及同版官方注释中与Screen Customization/Ribbon/快速访问工具栏可见性有关的条款。重点核对ACCESS_AVAILABLE是否表示可列出/可放置、定制界面调用access的时机，以及SDK指定成功和当前模式列表之间有哪些有文档依据的限制。

现有三个命令都使用同一readonly_ping_access并返回ACCESS_AVAILABLE，这已经是源码事实，不假设body得到不同状态。获取这些契约是为了选择下一个可解释的观测点，而不是直接修改access或猜隐藏状态。若帮助没有说明定制时机/模式，明确写未说明，不按经验补规则。既有Designate声明、消息文件和版本信息不用再次提供。

拿到片段后再定一份最小诊断补丁：优先考虑有真实契约支撑的按名查询/ID一致性观测。查询若只返回注册ID，不能把它包装成UI列表/Designate元数据回读。不要在同轮夹带名称、标签、注册顺序或回调返回值修改。

## 4. 加载方式与后续判断

本次报告明确记录手动DAT对话框不响应后，使用经本机授权的config.pro protkdat自动加载，与早期手动加载不同。这是跨轮比较的额外变量；不能把全部差异都归因于新增两个探针。同一R7运行中ModelInfo可见且描述-32的对照事实仍然成立。

本包不追认或发起配置修改、不要求重测、重启、恢复/追加config.pro条目，也不据此猜加载时机就是根因。后续交接应持续记录实际加载方式与配置状态，避免静默改变实验条件。助手已记录的本机授权与备份单独保留。

若后续SDK查询支持注册ID一致性核验，结果只能按实际能力解读：查得同一ID证明该次查询身份一致，查不到/不同则产生注册上下文线索；两者都不直接证明UI可放置或缓存状态。access观测如果涉及高频回调，必须先设计有界日志，不能每次回调同步落盘。

## 5. 不做什么

不重做动作名长短/“匹配日志名”试验，不修改描述、分隔格式、消息文件名、label、access、优先级、标志或顺序；不新增未知SDK调用、rbn、菜单、图标、刷新或命令执行代码。

不点击面计数、不选择、打开、写入、保存或转换模型，不部署正式版本，不自动操作Creo或修改配置/缓存。没有新DLL构建/加载/UI试验。计数回调仍未验收，UI根因仍未定位。公开包不重发原始截图、用户路径、SDK内容或指针地址。
