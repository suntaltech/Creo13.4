# P1 第二十五步：被动环境身份采集工具包

日期：2026-11-09，Asia/Shanghai。R1修复dat引用行尾注释解析，保留引号内!/#并补记helper散列；不改变现场行为范围。R1修复dat引用行尾注释解析，保留引号内!/#并补记helper散列；不改变现场行为范围。交付物是PowerShell脚本与证据结构说明，不是Creo插件，不新增业务按钮。当前WFC转换路线继续暂停。本轮没有在Creo现场执行采集，也没有清理Step22/23锁或修改配置。

## 使用方式

在Windows PowerShell中，把下列示例路径替换为现场真实路径后运行。脚本面向Windows PowerShell 5.1或PowerShell 7；不依赖Python、Java启动或Creo SDK。示例路径不是已验证的现场部署事实。

```powershell
$params = @{
    OutputRoot       = 'D:\P1_EnvironmentEvidence'
    ConfigProPaths   = @('D:\实际工作目录\config.pro')
    PluginRoots      = @('D:\实际Step23部署目录\classes')
    OtkJarPaths      = @('D:\实际Creo目录\Common Files\text\java\otk.jar')
    PfcasyncJarPaths = @('D:\实际Creo目录\Common Files\text\java\pfcasync.jar')
    LogPaths         = @('D:\实际日志目录\jlink.log', 'D:\实际日志目录\stderr.log')
}
.\scripts\Collect-P1Environment.ps1 @params
```

参数说明：

- OutputRoot为专用证据输出目录，应在安装/插件扫描目录之外；每次创建新UTC时间+随机标识的目录和ZIP，不覆盖旧证据。
- ConfigProPaths必须明确给出；可同时列出实际需要核对的config.pro/config.sup。脚本不会把磁盘文件自动认定为本会话生效配置，启动时覆盖顺序仍需既有记录证明。
- PluginRoots可填目录或单个JAR；仅记录.class/.jar路径、大小、SHA256，不复制二进制。目录可含空格/中文。
- InstallRoots可选，限定在具体Creo安装目录；也会从观察到的Creo映像路径有限推断Common Files候选。拒绝整个盘符或用户主目录扫描；不跟随链接/重解析目录。
- OtkJarPaths/PfcasyncJarPaths优先使用已知真实文件路径；未提供时也会检查有限安装目录及dat的classpath。找不到、权限不足或来源不明就保留未知，不伪造实际加载路径。
- RegistryDatPaths可显式补充dat文件。脚本同时解析config中的protkdat/creotkdat/toolkit_registry_file启用和注释引用；相对路径先以配置文件目录为候选基准，并明确该基准不等于已证明的Creo启动工作目录。
- LogPaths仅接受已有.log/.txt/.err/.out/.jsonl。原始日志/转储不入包，模型文件不读取。需要崩溃摘要时提供已有文本诊断报告，不提供二进制dump。按需列入已存在的Step23 JSONL只生成事件计数/最后事件，不复制原件。
- TextCodePage默认0：识别BOM、尝试严格UTF-8，再使用当前文化ANSI代码页并标注回退。旧中文配置可显式指定936；原始字节散列和解码依据保留，不能把未知编码当作已核实。
- MaxTextBytes默认16MiB，超限跳过并在issues中记录，不悄悄截断。MaxScanEntries/MaxScanDirectories默认20000/3000，达到边界会标明覆盖不足。dat闭包最多读取200份快照，超限保留缺项记录。
- DiskOnly跳过进程查询，仅做磁盘身份与日志摘要；用于无法查询进程的情况，不能由其输出补填运行态事实。

脚本不主动提权、不更改执行策略、不附加调试器、不运行jcmd/jstack/java、不停止或重启Creo、不自动上传。现有执行环境拒绝脚本或读取时，按现场既有规则处理，不为采集修改Creo配置或许可。

## 输出

完成后显示本地ZIP路径及SHA256，并在OutputRoot中留下同名目录、ZIP和.zip.sha256。结构见docs/evidence-layout.md；先看scan-coverage.json与issues，不能仅凭“ZIP生成”认定字段齐全。

config.pro与dat按全部行读取，保留行顺序，但不输出未脱敏原文：普通注释、非环境配置值及敏感键值遮盖，环境相关路径/参数去除常见用户标识、邮件、凭据和模型引用。编码与换行会归一化；originalSnapshotSha256对应读取的原始字节，SHA256SUMS对应输出脱敏文件，二者本来就不应相同。“全文”在本工具中是完整行序列的脱敏快照，不是逐字节原件。

非环境值默认遮盖，避免把与SDK无关的个人/业务设置带出；对环境路径等保留字段，规则不能识别任意私人命名，manifest明确要求上传前审阅。脚本不自动发送任何内容，不把原始配置、日志、模型、坐标、句柄、内存转储、SDK或插件二进制打包。无需把完整许可文件、密钥或所有环境变量交回。

## 能证明什么

SDK/插件哈希只是读取时的磁盘身份，不是“当前JVM已经加载它”。已有类加载日志可提供部分CodeSource线索；缺失日志不等于未加载。进程命令行仅提取-classpath/-cp/--class-path参数，不保存完整命令行或模型参数。

Windows进程查询仅检查xtop.exe/parametric.exe/proe.exe及其直接Java子进程，记录映像路径/版本、相关模块元数据；不采集进程所有者、其他Java程序或完整环境。安装目录中的Creo候选可执行文件也只读散列/版本资源，不执行；进程未运行时这些只是磁盘安装身份。若可枚举jvm.dll，则记录映射路径、文件版本/公司、PE位数以及附近release文件的限定字段。这是“已观察到JVM模块路径+邻近文件信息”，不是Attach读取的实时java.home/System Properties；磁盘文件也可能已被替换。

模块枚举顺序不是加载时间顺序。真正加载先后只从已有、可识别的类加载日志保留局部顺序；没有就UNKNOWN。配置注释引用不是活动模块，活动配置行也不证明该dat已被本会话加载。许可/注册官方支持结论始终UNVERIFIED，采集后再离线判断。

## 验证状态

- 在云端PowerShell 7.4.6/Linux x64通过42项纯文件/脱敏/解析断言；临时文件端到端ZIP生成、完整性与内容检查通过。
- 已验证输入文件全部哈希不变、启用/注释dat、带空格和中文路径、显式936编码、大小边界、链接目录跳过、根目录扫描拒绝、输出与输入扫描根重叠拒绝。
- ZIP中未发现测试秘密/用户资料/模型/坐标/句柄/原始日志或二进制；PowerShell AST解析及禁止命令/SDK引用检查通过。
- 合成文件只用于文件I/O测试，不加载SDK、不构造模型对象。Windows CIM/进程模块分支和Windows PowerShell 5.1尚未实际执行；现场进程权限、JVM版本与采集完整性仍待现场结果，不宣称已兼容认证。

可选复核：tests/Run-Fixtures.py需要Python 3和可信PowerShell宿主，使用临时文件且-DiskOnly；生产采集脚本无需Python。测试命令：

```text
python tests/Run-Fixtures.py --pwsh /path/to/pwsh
```

本包不携带PowerShell安装包、SDK、JDK或Creo模型。evidence/verification.json和static-ast-validation.json记录当前测试范围。
