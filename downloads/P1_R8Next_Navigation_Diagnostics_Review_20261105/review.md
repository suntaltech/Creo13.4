# R8 Next 导航诊断真机验证审核

日期：2026-11-05。审核用户上传r8next-navigation-diag-verify-20261105.zip。本轮仅离线核对与交付审核资料，不执行Creo、GUI、网络分析、模型操作或配置恢复。

## 结论

新日志支持诊断候选在SISWSheetMetal回调中按预期区分导航前URL探测失败和后续导航结果：PRE_NAVIGATION_URL_PROBE抛XToolkitGeneralError，随后SET_URL返回、CALLBACK_COMPLETION返回；截图显示目标Sheet Metal页面。可收束“GetURL探测失败后沿原策略继续导航”这一分支的受控观察，保留现有源码/二进制，不因此再生成新候选。

这是失败阶段澄清与日志改进的验证，不是底层GetURL异常已消失或已修复其根因。API事件始终pageLoad=UNVERIFIED；页面显示由截图提供补证，不将该字段改成已加载。preNavigationUrl=null符合探测抛异常后的原有策略。

## 本次新增证据

上传日志的前144178字节与上一三页面验证包日志完全一致；新增20338字节，原文693–744行，不把旧源码的页面回调当成本候选新覆盖。

启动UTC03:53:29（北京时间11:53:29）返回；新增SISWSheetMetal回调约UTC03:54:04至03:54:05（北京时间11:54:04至11:54:05）。实际requestId为1b4c4ced-dfee-4eaf-9e12-223e3936a638，由上传日志直接提取，未生成新ID。

| 原文行号 | 事件 | 说明 |
|---|---|---|
| 726 | PRE_NAVIGATION_URL_PROBE ATTEMPT | 开始读取导航前URL |
| 727 | PRE_NAVIGATION_URL_PROBE THROWN | XToolkitGeneralError，真实堆栈紧随其后 |
| 728–737 | 异常堆栈 | 包含Window.GetURL及候选OnCommand位置；堆栈本身无requestId，按连续上下文关联 |
| 738–739 | SET_URL ATTEMPT / RETURNED | 后续导航调用已返回，不与探测失败混为一谈 |
| 741–742 | CALLBACK_COMPLETION RETURNED及人读日志 | 明确preNavigationUrl、target、pageLoad=UNVERIFIED和回调返回 |
| 743–744 | stop entered / returned | 停止流程返回 |

同一段包含四命令的20条注册事件，逐项提取见audit.json。截图地址与日志目标sheetmetal.html?modelMode=solid_part一致，正文仍是先分析提示；未提供分析后结果，不能说已执行只读分析。

## 身份与声明边界

操作者报告使用新编译classes；PAGE_NAVIGATION事件、阶段和日志文案与已交付候选一致，支持新诊断逻辑确已出现。此包未附实际加载class/dat/资源散列，不能单凭文案独立认证加载二进制与交付逐字节相同。参考候选来自提交460b689a40cd1914279960d7361112f98e930a6e，manifest散列在本审核记录中保留。

未点击原生转换、未保存模型、未修改Ribbon、config.pro已恢复均作为操作者声明记录，不据静态截图或缺少日志自动认证模型和配置状态。本轮没有执行这些操作或补填规范运行记录PASS。

## 尚未覆盖的分支

同页SKIPPED_SAME_URL、探测正常返回URL/null、SetURL抛异常、窗口尺寸/激活/刷新失败均未在本次新二进制段观察到。此前旧源码的GetURL成功不能直接算成本次二进制覆盖。保留未观察状态，不为了凑覆盖去强制制造异常，也不以本次成功宣称所有导航分支已通过。

## 建议下一步

导航诊断本轮主目标已有受控证据，先冻结本候选。接下来按既有只读分析计划核对实际sheetmetal.html、sheetmetal.js、core.js及读取接口身份；满足只读门禁且有相应GUI授权后，再验证“分析当前零件”。Web分析审计事件与页面导航日志分开关联，不把PAGE_NAVIGATION成功当作分析成功。

继续不点SISWSheetMetalConvert、不保存模型、不修改Ribbon持久化；本次离线交付授权不扩大真机动作范围。资源、SDK、源码和二进制都不改，不发布新安装包或Release。

本次公开材料仅含审核说明、事件提取、原件散列及校验文件，不重新发布完整日志或原截图。
