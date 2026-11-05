# R8后导航诊断源码与只读分析准备候选

日期：2026-11-05。按用户持续授权处理本次任务单的离线工作；任务单中的真机分析为准备事项，不在云端执行。

## 交付和二进制选择

本包是纯离线源码/资料候选，不是安装包或正式发布。本包没有class/JAR，明确沿用现有R8 classes作为当前受控运行基线。R8资源9项原字节随包提供，沿用R8类路径/配置不自动切换到本源码。本次不部署、不改生产配置、Ribbon或模型。

source/BatchRename.java及patches/navigation-diagnostics.patch是尚未编译的诊断候选。继续使用R8 classes时，新增PAGE_NAVIGATION日志不会出现；不能声称源码已修复或新二进制验证通过。若要启用新日志，必须在本机真实Creo13.4 SDK上将候选及全部内部类编译到独立新目录，单独核对class身份和受控配置后执行另行授权的验证，不能混入旧内部类。本轮未运行javac、javap、jar、Node、Mock/Stub或HTTP运行测试。

## 与R8的差异

仅修改页面导航诊断：保留GetURL和同页不重复SetURL条件；将pre-navigation探测失败与SetURL/窗口激活/刷新失败分开，日志标注preNavigationUrl并保留异常堆栈和实际回调requestId。API完成不宣称页面加载完成，pageLoad固定UNVERIFIED。底层GetURL异常原因未被修复。

入口注册、启动/停止、三个页面路由、原生转换实现、浏览器转换桥状态、其他业务逻辑及R8 working-text资源均不改。source包含既有源码内容，继承既有部署默认值；本候选未新增本机绝对路径。参考Web文件只用于解释历史只读契约，不部署、不认证当前网页实现。

[导航日志规则](docs/navigation-events.md)说明预期字段及分支；[只读分析计划](docs/readonly-analysis-plan.md)说明实际Web身份门禁、前端事件来源、页面反馈、允许读取集合和写模型越界。本轮不点击SISWSheetMetalConvert、不保存模型、不发布版本/安装包、不修改Ribbon持久化。

manifest给出原始任务单、业务基线、候选源码、R8资源及历史Web来源身份；SHA256SUMS覆盖包内所有交付文件。validation是文本差分、补丁精确重放及资源散列核对，不是编译/运行结论。真实日志和分析结果应在未来授权会话产生，本包未填任何运行PASS或虚构ID。
