# P1 JDK磁盘身份补充审核

2026-11-09，Asia/Shanghai。仅审核用户提供的P1_JDK_Disk_Identity_20261109.md及此前环境采集结果；没有新插件、采集器或现场操作。

## 本次补齐

文档报告config.pro指向D:\creoson\jdk25\jdk-25.0.5+8\bin\java.exe，与上轮采集到的jlink_java_command路径一致。release字段声明：Eclipse Adoptium、Temurin-25.0.5+8、Java 25.0.5、运行时25.0.5+8-LTS、Hotspot、Windows、x86_64、IMAGE_TYPE=JDK。

jvm.dll报告位置为该JDK的bin/server/jvm.dll，大小15489656字节，SHA256归一化为3db3f5cf8ea1a5798783731fb9ad36d8b425b9d69d676ef07d0b40d709cbde1c。散列格式有效；本轮只有Markdown中的摘录，没有原release文件或DLL，因此云端没有独立复算DLL散列，也没有读取PE头来独立认证位数。以上字段按用户提供的只读元数据记录，不能升级为本机二进制鉴定或运行态事实。

## 兼容性判断更新

此前已比对现场磁盘SDK与离线副本一致，otk.jar大多数类为major69，对应Java25类文件格式。此处声明的Java25满足这一个代际条件；“配置所指磁盘JDK明显过旧、不能理解SDK字节码”的假设优先级下降。但字节码版本符合只是必要条件，不证明全部依赖、PTC本地模块或官方运行条件成立。

SDK Manifest的Created-By: 25 (Oracle Corporation)是构建元数据，不是“必须使用Oracle JVM”的运行许可或兼容约束。不能仅因磁盘JDK为Adoptium就判定不兼容，也不能据此承诺Temurin已获该Creo维护版本的PTC认证。需要同版本官方支持矩阵/运行说明；当前未提供，不能建议直接替换供应商、降级JDK或改启动配置。

## 仍未补齐

两轮环境采集均未观察到Creo进程；本次说明也明确没有启动Creo。因而故障会话实际加载的JVM路径、版本、供应商、位数和本地模块关系仍未知。此JDK是否在故障时存在/生效、实际启动器和有效配置覆盖顺序，不能由当前磁盘路径单独证明。

OTK许可、注册/初始化契约、内部WFC视图是否由官方支持，以及原生卡死根因也未由该文档解决。PowerShell版本与JDK磁盘版本均不能代替故障会话JVM身份。没有新增stderr/崩溃栈或实际加载来源，不能把“JDK资料补齐”写成“转换前提通过”。

## 后续顺序

保留稳定Java界面/PFC只读链路，冻结已验证的采集器R1，当前WFC ElementTree工厂/转换仍暂停。优先核对现有启动记录与准确Creo版本的官方JDK/OTK注册、初始化、线程和合法运行许可条件；如已有运行中的受控会话，再在同一被动采集范围补实际JVM模块身份。

不为填字段主动重启、不重试Element/Elements工厂、不改startup、不加pfcasync、不更换JDK或清锁。仅在支持条件有据或不能满足时，再决定继续官方支持的Java OTK评估或有前置条件的C/C++转换内核；本轮没有证据要求直接换语言。

本审核包含Markdown结论、机器可读review.json、manifest和SHA256SUMS，不包含原始JDK/SDK/DLL、配置或现场日志。文档标题中的“R2补采”按补充证据处理，不代表本轮生成采集器R2。
