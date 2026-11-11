# Step41 R7：补齐两个可见命令的描述解析对照

日期：2026-11-11（北京时间）。本轮只增加两个消息解析调用，不修资源、不改计数或UI注册。

## 1. 改了什么文件

仅 src/entry.cpp：在 probe_designation_messages() 原四次调用之后增加：

```cpp
ProFileName ping_file = L"p1_readonly_ping.txt";
ProCharLine control_description_key = "P1_READONLY_MODEL_INFO_DESC";
ProCharLine ping_description_key = "P1_READONLY_PING_DESCRIPTION";
probe_one_message("control_model_info_description", control_file, control_description_key);
probe_one_message("control_ping_description", ping_file, ping_description_key);
```

同时把原 helper 注释中的“All four”改为“These”，不改 helper 实现。新增两个调用复用同一有界缓冲、JSON编码和日志；每次初始化合计六次，各一次，不循环重试。原四次顺序、字段和返回事件完全保留。

基线为上传且与交付版字节一致的 R6：e3a554a9562badc7430551b719159c8d3d63eb63d0a868341ed4e5f547b13f48。完整源码与 patches/description-controls.diff 等价；隔离副本核对基线后 git apply --unidiff-zero --check，通过后 git apply --unidiff-zero。

## 2. 为什么改

R6 外部校验与内部16项清单通过。原始日志最新 pid=16316/load_token=17897531：六项注册返回0，三个ID不同，body label/help 与 ModelInfo label 解析0且文本符合资源，body description 返回-32，截断到80宽单元。截图仍无面计数项，没有其回调事件。

这证明面计数标签/帮助在该次探针上下文可以解析，不能再把它们完全不可获得当成唯一解释；不证明此前Designate命中的精确路径。-32是ToBuffer容量观测，不足以证明资源对Designate无效或导致列表隐藏。可见的ModelInfo描述源行82字符，未由R6对其描述做SDK对照，本轮补齐这项缺口，并加入73字符的Ping描述对照。

仅有源文件长度不等同于SDK实际译文长度；当前语言、既有加载状态或缓存仍需通过结果文本分辨。两个新增资源值已核对不含变参替换占位符，继续按真实契约三参数调用。

回传报告关于“SDK动作名与内部command名不同”的判断需限定：facts.command是日志功能标识，不是SDK的另一个命令名参数。R3以前两者相同也已不可见，不能据此增加改名试验。实际显示标签是另外一项元数据，但本轮不改它。

## 3. 预期验收

本机助手按固定工具链离线重编、链接并核对源码/DLL哈希、导出/依赖和两个plus引用，保持PRO_USE_VAR_ARGS在所有SDK头文件之前。不要改入口、SDK头文件或构建策略，不重复环境采集。

六个资源均保持R6字节，哈希见evidence/manifest.json。本包不附替代资源。按既有隔离候选流程由用户手动一次受控加载和相同条件的列表观察，不部署正式版本，不点击面计数、不打开/切换/保存模型。

日志中的revision仍是 UI_R6_MESSAGE_PROBE，身份日志仍是 UI_R4_R1_DIAG，均为有意保留；R7身份用新源码/DLL哈希及两个新增role确认。预期六组BEGIN/RETURN出现在三项原注册函数之后、INIT_BEFORE_RETURN之前：原四组，然后 control_model_info_description、control_ping_description。新结果字段与原结果一致，仍记录原始错误码，不强制预设0。

若采用当前ASCII源文本，ModelInfo描述82字符可能同样-32且wide_units=80，Ping描述73字符可能返回0。这是预期参照，不替代实际文本/返回码。对照解析成功也不是UI或面计数功能通过。仅回传最新初始化、必要截图及acceptance.template.json，不重采相同SDK资料。

## 4. 结果分支

- 构建失败：回传真实诊断，不进入现场操作，不改回调/强制转换绕过。
- ModelInfo描述也-32但其命令仍可见：说明“描述超长必然导致隐藏”不能作为当前工程的充分解释；不是证明超长在任何命令/路径下都无影响。不继续把-32直接当UI根因。
- ModelInfo描述返回0：先看实际文本和长度，可能与磁盘82字符不同；不能据此直接断言body的-32导致隐藏。需要单独资源短描述试验时，另做单变量补丁，不在本轮夹带。
- Ping描述返回0且文本/长度符合：确认同一helper短描述对照有效；若它也异常，优先分析解析上下文/当前译文与原始错误码，不修改业务代码。
- 对照失败或文本不同：按文件、消息、译文、格式、重复文件/内存、截断等原始码分别记录，精确路径仍为未知。
- BEGIN有而RETURN无：定位最后role/key，保留证据，不自动重试。
- 命令突然可见：记录观测干扰可能，不将增加诊断调用当作稳定修复；不扩展点击试验。

缓存/语言目录不是该接口输出，成功文本不反推出解析路径，也不证明Designate先前使用同一缓存。所有解析在指定之后执行仍可能影响后续可见性。

## 5. 不做什么与检查边界

不改任何资源值/格式、动作名、显示标签、消息键、注册顺序/标志/优先级/access、三个业务回调、计数、日志存储框架、入口返回或编译宏；不新增SDK API、菜单、图标、刷新、别名、rbn、消息弹窗或输入流程。不改DAT语义/config.pro、缓存/语言设置、CRT或既有链接要求；不选面、写入、保存、转换模型，不自动启动停止Creo。

云端已验证输入校验、源码匹配、补丁应用及还原后逐字节等于R6，六次调用顺序与新增键/无占位符契约也已核对。编码helper没改，已有测试不重复运行。没有云端真实SDK编译、DLL加载或UI验证。原始截图、本机绝对路径、SDK摘录和日志地址值不随公开交付重发。
