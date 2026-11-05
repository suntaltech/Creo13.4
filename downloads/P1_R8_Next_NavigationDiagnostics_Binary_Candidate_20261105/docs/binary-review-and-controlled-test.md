# 二进制审核与未来受控验证

本包已由真实otk.jar编译，不再是仅源码计划包。当前编译使用javac25.0.5.0.3、--release17、UTF-8、-proc:none，classpath仅otk.jar。所有4个class major61、无preview；这不代表可在Java17执行：用户SDK多数依赖类major69，需要能加载这些类的Java25运行环境。发布者未核实PTC官方支持矩阵；沿用已核实的本机运行前提，不自行降级JDK。

SDK原件仅保留云端编译输入，未打进交付包或插件JAR；pfcasync.jar被接收但明确排除classpath。不使用Mock/Stub，不调用Creo接口验证，不把Java编译等同真机启动或模型功能通过。SDK来源/Creo13.4归属依用户上传说明，散列可核对，不伪称已从安装目录独立认证。

## 本机先静态审核

先校验外部ZIP.sha256，再校验内部SHA256SUMS与manifest。核对候选source SHA、SDK SHA、4个class SHA和JAR；JAR内class须与classes/逐字节一致。类名包含主类和3个内部类，不混用R8旧内部类。evidence含真实编译stdout/stderr（为空表示该流无输出）、命令和退出码、javap反汇编及重复编译一致性结果。

优先使用已验证路径形式：将classes/作为独立新目录，registration模板的java_app_classpath填为该目录，text_dir填为本候选working-text-layout绝对目录。占位符不可原样加载。本轮不修改生产dat、config.pro、服务启动脚本、原R8目录或持久化Ribbon。

插件JAR是同一4个class的薄归档，未含SDK或消息资源，也无Main-Class；不使用java -jar执行。提供JAR用于独立交付/核对；如需改为JAR类路径，应另行核实本机注册方式，不将它当安装包。

## 未来单独授权的回调验证

正常退出旧受控进程后使用独立新class目录，避免同名类已被旧classloader加载。核对实际JVM、SDK、class/资源及dat身份。只验证页面回调，不点击原生转换，不点击模型写按钮、不Save、不Add或修改持久化布局。

按navigation-events.md检查PRE_NAVIGATION_URL_PROBE与SET_URL等事件，同一真实requestId贯穿；GetURL异常时保留真实堆栈且后续导航结果分开记录。若同页URL可正常读回，应看到SKIPPED_SAME_URL；无法观察的分支记未验证，不制造异常。API完成始终不代替页面截图；导航前变量已明确标记preNavigationUrl。

源码的启动流程仍有既有CREOSON异步启动能力，原生转换实现仍存在但本轮不点；不能把“只验证页面”说成整个插件不存在其他副作用。配置恢复按本机既有授权独立留证，本包不执行恢复。

## 只读分析准备

docs/readonly-analysis-plan.md保留有效。reference-only-web仍是历史参考，不应覆盖现有Web；先核对实际页面/脚本及调用集合，再在独立授权下点击分析。分析事件在浏览器SiswCore.events，不是新增PAGE_NAVIGATION日志，不混淆Web分析ID与页面回调ID。当前编译成功不证明实际页面只读或原生转换可用。
