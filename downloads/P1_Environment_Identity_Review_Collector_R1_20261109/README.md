# P1 Step25 环境身份审核与采集器R1

2026-11-09，Asia/Shanghai。本包审核用户提供的环境证据，并修复一个只读路径解析缺陷。没有在现场执行、重启Creo、改dat/config、删锁或重试工厂；不恢复转换。

## 已确认的磁盘事实

- 原ZIP完整性和全部内部SHA256校验通过。采集窗口为上海时间2026-11-09 14:55:27至14:55:52。
- 安装目录候选中的parametric.exe和xtop.exe产品版本均13.4.0.0、x64，保留原文件版本/哈希；这证明读取到的安装镜像身份，不证明当时正在运行它们。
- 现场磁盘otk.jar（4362992字节）和pfcasync.jar（1207086字节）与此前上传的SDK副本散列完全一致。
- Step23的16个磁盘class全部匹配交付提交eb8b330519fb8bf91cc0d973da09c99632e212f1，未发现磁盘插件版本不一致。另有BatchRename的4个class，但没有其已加载身份认证。
- 采集主脚本SHA匹配Step25交付；旧证据未记录Collector.Core.ps1的散列，不能将主脚本匹配升级为整个工具链的加载认证。R1已补主/helper双散列。

sdk-comparison.json和step23-class-comparison.json给出逐项比对。它们降低“拿错磁盘SDK或Step23 class”的优先级，不能证明JVM已经加载这些文件或排除启动classpath中的另一份SDK。

## 配置快照

Common Files/text/config.pro的磁盘快照包含jlink_java2 on和jlink_java_command D:\creoson\jdk25\jdk-25.0.5+8\bin\java.exe。该路径配置说明选用意图，不证明执行过、供应商/位数或实际java.home；文件夹名字也不能代替版本读取。

同一快照中只有Step23 protkdat行未被注释，BatchRename和其他历史引用为注释行。Step23实际提供的dat快照为name=p1s23、startup java、java_app_class=p1.controlled.step23.P1RootElementProbeStep23；java_app_classpath指向其classes目录，text_dir指向resources/text。所采Step23与BatchRename dat的classpath均未显式列入pfcasync.jar。不能由此排除其他配置/引导加载器，也不能因为磁盘存在两个SDK JAR就判定混装。

其他配置快照包括安装默认、ORIG与示例文件。扫描到某份config.pro不表示它对当前/此前故障会话生效；本包未认证覆盖顺序。startup java已从“模板”推进为“实际提供的磁盘dat内容”，但仍未证明故障会话加载了它，也没有官方全文/许可结论来断言应直接切换otk_java。

## 仍缺少的运行态证据

processes-and-jvm.json为NO_CREO_PROCESS_OBSERVED，records为空，diskOnly=false。这只能说明采集器在当次查询未观察到目标进程，不能证明此前会话必然崩溃、何时退出或为什么退出。没有宿主JVM版本/供应商/位数、已映射jvm.dll、实际已加载SDK/插件CodeSource、启动classpath及真实加载顺序。

PowerShell 7.6.4是采集器宿主，不是Creo JVM。已给出的J-Link日志摘要有1056行、206774读取字节，但没有识别到类加载记录或限定错误标记；这不等于没有错误、许可满足或日志就是Step23故障会话。未提供原生stderr/崩溃栈及可靠会话关联。

因此当前不能判定SDK/JVM/classpath混装、OTK缺许可、注册模式不合法或已知13.4缺陷。内部WFC反射视图的官方支持契约仍是高优先级未决项，普通PFC成功和当前磁盘身份匹配不能代替该契约。

## 本次发现并修复的采集器缺陷

11条历史dat引用的路径带有行尾注释。原采集器把注释也当作路径，产生UNSUPPORTED_INPUT_NOT_READ和existsAtCollection=false；这些结果不能据此解释为真实dat文件不存在。原包保留不改，审核明确这11项未采到。

collector-r1/只修只读解析与来源记录：识别引号外、空白分隔的!/#行尾注释；保留引号内的合法字符，无法配对的引号保持未知；兼容整段引号和逐项引号classpath；记录主脚本和helper散列。无Creo API、WFC对象、业务按钮、配置修改或启动/重启功能。

R1通过42项纯文件/脱敏/解析断言及端到端打包回归，包括本次行尾注释问题、含空格/中文路径、原输入哈希不变、ZIP内部校验、盘符/用户根拒绝、链接跳过和200份dat上限。测试在云端PowerShell 7.4.6/Linux临时文件完成，没有现场运行R1，也未认证Windows JVM模块分支或PowerShell 5.1。

安装目录扫描达到20000条目边界（记录entriesSeen=20001），因此不能声称找齐全部安装配置。本次SDK、两个安装镜像和目标16 class已有直接身份记录；无需为扫描完整性盲目提高全目录上限，优先显式指定SDK/插件/配置/dat路径。

## 下一步

继续保留稳定Java UI/PFC只读链路，暂停当前WFC建树/转换。先使用已有启动器/日志、官方同版本注册/初始化/许可说明和配置来源核对支持条件；采集后不自动更换JDK、加pfcasync或改startup。

若现场已有运行中的受控会话，可在后续同一被动采集范围补进程/JVM身份；若进程已退出，本包不要求为填表重启，也不再次点击Step22/23。R1可用于补读被注释尾缀挡住的磁盘dat资料，不需要调用任何Creo按钮。此后再决定能否回到官方支持的Java OTK路径，或按匹配SDK/ABI/许可/官方示例条件评估C/C++转换内核；不凭当前缺项直接宣布必须换语言。

使用collector-r1/README.md填写真实路径，输出仍为本地脱敏证据，不自动上传。当前审核包不重分发输入的环境ZIP、配置快照、原始日志、SDK/插件二进制、模型或运行时地址。
