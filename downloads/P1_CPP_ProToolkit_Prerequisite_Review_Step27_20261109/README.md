# P1 第二十七步：C/C++ Pro/TOOLKIT前置评估

2026-11-09，Asia/Shanghai。判断：C/C++可以作为转换内核候选进入离线前置评估，但当前资料不足以认定SDK、编译链、许可或真实转换可用。保留Java UI和已验证PFC只读层，P1写模型目标继续暂停；本轮没有C/C++实现、DLL、编译链接、部署、现场配置变更或Creo启动。

## 1. 开发所需SDK、编译器与ABI

这里评估的是“用C/C++工程调用公开Pro/TOOLKIT C API”的原生模块，不自动等同于另一个Object TOOLKIT C++产品/库族。Java的otk.jar/pfcasync.jar不能替代C头文件/导入库，Java OTK许可也不能自动证明C/C++ Pro/TOOLKIT许可就绪。

| 项目 | 需要核对的内容 | 当前状态 |
|---|---|---|
| 基础SDK | 本版ProToolkit.h、其依赖头及完整include目录；输入中的protoolkit.h大小写不作为Windows缺文件依据 | 未提供文件清单 |
| 模型/体/几何/选择 | ProMdl.h、ProSolid.h、ProModelitem.h、ProSelection.h、ProSurface.h；多实体体接口及对应头（如本版提供ProSolidBody.h） | 候选，须以13.4实际SDK核定 |
| 特征/树/值/数组 | ProFeature.h、ProElement.h、ProElemId.h、ProValue.h、ProArray.h，以及本版类型枚举/单位相关依赖 | 候选，未核对原型 |
| 钣金驱动面说明 | 同版本ProSmtDrvSurf/驱动面树文档与实际元素ID定义；ProSmtDrvSurf.h若本版提供则核对 | 有历史文档摘要，无原头/全文 |
| PTC导入库 | 当前模式对应的Windows x64 Pro/TOOLKIT导入库。可查protk_dll*.lib库族（如protk_dll.lib或带后缀变体），确切名称/组合由本版官方makefile决定 | 未提供，不声明示例名在本机存在或需要同时链接 |
| 其他链接依赖 | Windows系统库、CRT及PTC其他依赖按同版本官方样例/库变体选取 | 未验证，不预写链接参数 |
| 编译器版本 | PTC对Creo 13.4准确维护版本支持的Windows MSVC/Visual Studio、工具集及Windows SDK版本 | UNKNOWN，不能凭经验指定VS2019/2022某版本 |
| ABI | Windows x64、官方C入口/回调签名、符号导出/调用约定、结构体对齐/宏、字符编码、CRT与内存所有权规则 | 未与本版官方样例核对 |

仅有“Visual Studio能编译C++”不等于受支持。Linux GCC、MinGW或其他SDK版本的.lib不能直接当作本项目Windows原生模块的验证环境。/MD或/MT、调试/发布库变体、编译宏和系统库均不在缺少官方makefile时猜测。

Windows x64为LLP64，指针64位而C long仍32位；不要把Java long句柄强转为C SDK对象或复制内部结构。使用ProMdl/ProSelection/本版体类型等公开声明，SDK分配资源按其公开释放函数配对；不混用CRT free/new处理SDK拥有的内存。用C++编译时，初始化/终止入口的C链接、精确原型和导出规则以官方样例为准，本轮不发明函数声明。

## 2. 本机是否已有完整SDK

已知磁盘证据中确实存在：

D:\Program Files\PTC\Creo 13.4.0.0\Common Files\protoolkit\protk_appls\pt_userguide\config.pro

它证明有Pro/TOOLKIT示例目录内容，不证明完整开发SDK、头文件、导入库、编译器或许可齐备。此前采集只按设计记录配置、Java SDK和插件身份，安装扫描还触及20000条目上限；没有核查.h/.lib，不能把“清单里没有”解释为“本机未安装”。云端工作区也没有ProToolkit.h/ProFeature.h或目标导入库，故无法进行真实C SDK编译核验。

因此三项当前结论均为UNKNOWN：基础头是否存在、Pro/TOOLKIT导入库是否匹配x64/维护版本、钣金/特征头与树说明是否齐全。常见查找候选为Common Files/protoolkit下的includes、平台obj/lib及protk_appls目录；实际树可能随安装版本不同，先只读列目录，不采购或覆盖安装。

## 3. C/C++是否同样需要许可

需要按Pro/TOOLKIT产品、开发/测试与分发/运行场景核对合法授权，以及官方解锁/签名/注册条件。不能假定C++免费、Java许可覆盖C++、有库文件就是已授权，或解锁等于取得许可。具体许可feature名、是否允许分发及运行条件须由PTC同版本说明和现场既有合法状态摘要确认；本轮没有许可证明，也不输出解锁/签名命令或密钥。

C++可能仍到达同一Creo原生建模核心，换语言不能证明Java工厂卡点会消失，更不能替代许可、ABI和受支持线程/初始化条件。

## 4. 官方注册机制

传统同步Pro/TOOLKIT原生模块一般使用注册文件机制，常见名为protk.dat；在支持的显式注册方式下文件名可以不同。同步DLL常见字段语义包括name、startup dll、exec_file、text_dir等，初始化/终止由本版SDK入口约定处理。确切字段、配置参数、搜索顺序及是否需要其他产品标记，必须对照13.4对应官方说明与样例。

本报告不提供可直接启用的dat，不复用Java的startup java/otk_java作为C模块注册结论，不改当前Step23注册或config.pro。异步/独立进程是另一运行方式，不作为本轮默认替代；“能找到protk.dat”不证明模块支持契约通过。

## 5. 驱动面转钣金的最小API能力

下面是能力与待核对候选符号，不是已验证的Creo 13.4调用表或可编译原型。每个函数、头文件归属、参数类型和许可条件，需待原始SDK/官方文档到位后逐项确认，绝不提供Stub补齐缺失符号。

| 阶段 | 最小能力 | 候选示例与边界 |
|---|---|---|
| 模型 | 当前模型、类型、身份/版本上下文与单位，只接受受控普通零件 | ProMdlCurrentGet、ProMdlTypeGet、ProMdlNameGet；版本/单位接口按本版头核对 |
| 体 | 枚举当前模型体、取得唯一默认体、稳定体身份与非钣金状态 | 本版公开多实体体接口；ProSolidDefaultBodyGet仅作为待查符号，不在缺头文件时给出原型或反射/地址替代 |
| 面 | 取得单一曲面选择、验证所属模型/当前体/类型/可见性，保持原始选择 | ProSelect、ProSelectionModelitemGet、ProSelectionAlloc等作为候选；用本版选择缓冲或选择方式，禁止传入Java内部指针 |
| 内存树 | 分配根/子元素、赋整数/厚度/引用值、挂接、释放资源 | ProElementAlloc、ProElemtreeElementAdd、ProElementIntegerSet/ProElementDoubleSet或本版ProValue方式；选择/引用赋值的准确接口待核对 |
| 创建 | 按官方模式提交特征树、取得错误/诊断与创建结果 | ProFeatureWithoptionsCreate作为候选；不能直接照搬WCreateFeature参数或猜测创建选项 |
| 再生 | 一次显式再生与错误返回 | ProSolidRegenerate作为候选；标志、隐式再生行为与错误处理按本版文档 |
| 回读/资源 | 原生特征身份/类型/状态、实际体钣金状态、树证据和资源归属 | ProFeatureTypeGet/ProFeatureStatusGet等为候选，体状态API待核对；ProElementFree/ProSelectionFree/ProArrayFree等按实际分配方式配对 |

历史驱动面方向来自用户官方文档摘要：根FEATURE_TREE、FEATURE_TYPE使用C枚举PRO_FEAT_DATUM_SURF，DTM_SRF_TYPE使用PRO_DTM_SRF_AS_WALL，目标体/驱动面/显式厚度对应相关SMT元素。不能复用Java FeatureType.getValue整数或硬编码Java/C编号；名称与结构应从C头及ProSmtDrvSurf同版本说明确认。材料侧、方向翻转、单位、目标体引用形式及必填元素仍需官方示例，不把此前Java七元素树当作C版合法树认证。

## 6. 第一版最小受控验证

本轮只定义将来验证范围，不构建或部署DLL：

1. 先完成SDK/编译器/许可/官方注册与入口原型核对，离线编译/链接审核；未通过不能进入现场。
2. 第一版仅验证官方初始化/终止与受支持回调，然后只读取得当前模型类型、单位、唯一默认体、一个被确认曲面的作用域/身份及必要状态。不分配业务特征树、不创建、不再生、不保存，不做任何旧WFC调用。
3. 每个只读边界先强制落盘START，再调用，返回写DONE及真实状态；审计失败则不执行。异常/错误即止，不自动重试，不输出XYZ/UV或原生地址。若没有受控目标/选择就只记录阻断。
4. 只有原生只读路径和支持条件都稳定，才另立“内存根/树构造并释放”的任务；再之后才讨论两次确认、明确厚度及独立一次锁的一次创建/显式再生。不把读接口通过当作转换成功。

第一版的通过标准是“受支持入口与只读身份链路有证据”，不是“能创建钣金壁”。C++仍可能出现原生不返回，因此不会直接重做一次大范围转换探针。

## 7. 条件不齐时的选择

先做一次限定目录只读SDK清单和官方平台/许可材料核对，而非立即采购。若已有完整SDK，只是未采到，则补证据；若安装确缺组件且已有合法授权，则按PTC正式安装/IT流程补组件；若授权缺失或不覆盖目标模式，再评估采购和预算。没有明确合法授权或支持工具链前，P1写模型目标继续暂停，保留Java界面与只读价值，不以宏、内部符号、假指令或修改JDK绕过。

## 可选的只读SDK清单脚本

scripts/Get-ProToolkitSdkInventory.ps1仅在明确指定的SDK目录读取文件名/大小/SHA256，记录.h/.lib/构建文件及平台说明的元数据，不复制SDK正文、不读取模型内容、不加载库、不查询许可、不执行编译器或Creo。默认最多20000条目/3000目录，拒绝盘符/用户根和链接遍历；覆盖不足时不宣称文件缺失或SDK齐备。输出根必须在输入SDK目录之外。

```powershell
.\scripts\Get-ProToolkitSdkInventory.ps1 `
  -ProToolkitRoot 'D:\Program Files\PTC\Creo 13.4.0.0\Common Files\protoolkit' `
  -OutputRoot 'D:\P1_CPP_SDK_Evidence'
```

路径来自历史安装位置，运行前按实际目录填写。本轮没有在现场执行此脚本；它不是DLL或Creo按钮。清单只证明文件名/磁盘身份，不认证库架构、导入符号、MSVC版本或许可。平台说明/官方makefile的必要全文需后续另做离线核对，脚本只记其元数据。

脚本在PowerShell 7.4.6/Linux临时文件通过解析与端到端测试：输入哈希不变、中文/空格目录、链接跳过、输出重叠拒绝、ZIP不含.h/.lib/模型内容、targetReady保持false。没有真实Windows SDK、编译器、链接或现场测试；测试文件不是SDK头/库Stub。

## 证据限制

现有原始环境采集可确认安装镜像13.4.0.0/x64、Java SDK和插件磁盘身份，以及Pro/TOOLKIT示例配置存在。没有准确版本官方C SDK原头、库、平台支持矩阵、编译器信息或许可记录。上轮PTC官网正文获取受403限制，本轮不将历史摘要冒充已验证全文。详见prerequisite-matrix.json、api-candidates.json和evidence/source-register.json。
