# P1 Pro/TOOLKIT SDK清单审核

2026-11-09，Asia/Shanghai。已收到P1_CPP_SDK_Inventory_20261109T081225Z_e168d6c2.zip，ZIP完整性与内部清单SHA256通过。文件清单记录时间为上海时间16:13:36；包内只有sdk-inventory.json及其校验，没有SDK正文或二进制。

## 结论更新

关键SDK组件的磁盘存在性已有清单支持：13个重点头文件全部出现，并有8个库候选、官方Platform_and_Compiler_Support.html和Makefiles.html。可以推进到“现有SDK的针对性离线文档/原型核查”；当前没有理由仅以“未安装SDK”为由采购或重装。

这还不是可实施认证：准确Creo版本关联、头文件实际声明、库类型/COFF架构/链接变体、PTC支持的MSVC工具集、CRT/ABI及许可/解锁/签名均未核实。P1写模型仍暂停，保留Java UI/PFC只读层；Java WFC工厂路线不恢复。本轮不生成新采集器、C/C++代码、DLL或注册文件，不编译、部署、启动Creo或改现场配置。

## 观测到的文件

清单共521项：505个头文件、8个库候选、8个构建/文档元数据记录；无重复相对路径，均报告读取期间稳定。

13个重点头在includes下：ProToolkit.h、ProMdl.h、ProSolid.h、ProSolidBody.h、ProModelitem.h、ProSelection.h、ProSurface.h、ProFeature.h、ProElement.h、ProElemId.h、ProValue.h、ProArray.h、ProSmtDrvSurf.h。另有ProToolkitDll.h、ProSelbuffer.h、ProFeatType.h等入口/选择/枚举相关头。具体路径、大小、原清单报告的散列见sdk-components.json。

| 库候选相对路径 | 字节 |
|---|---:|
| x86e_win64/obj/protk_dll_NU.lib | 66698112 |
| x86e_win64/obj/protk_dllmd_NU.lib | 77319316 |
| x86e_win64/obj/protkmd_NU.lib | 61889148 |
| x86e_win64/obj/protoolkit_NU.lib | 76498306 |
| x86e_win64/obj/pt_asynchronous.lib | 108753438 |
| x86e_win64/obj/ptasyncmd.lib | 109781434 |
| x86e_win64/obj/ucore.lib | 1064736 |
| x86e_win64/obj/udata.lib | 1686 |

这些名字证明清单列出了多种库文件，不证明它们都是导入库，也不证明哪组适合本项目。x86e_win64是目录标记，本轮没有解析库机器类型/导入符号。不得由_NU后缀推断“免许可/免解锁”，不得仅由md后缀自动选定/MD，也不能把同步/异步和不同CRT变体全部混链。选择必须依据当前版本官方构建说明与真实Windows样例makefile。

## 已定位的两个最小官方输入

1. online_help/creo_toolkit/get_started/Platform_and_Compiler_Support.html，5066字节，SHA256为27226a525094ec1340648bf4ea73271b417f5708caa29137af95aacd7c4cb90a。
2. online_help/creo_toolkit/get_started/Makefiles.html，5282字节，SHA256为90fd24367afec9f18552de6b874a1b3216ad7bc20b5c0c3208e81d10dfa0a4d9。

下一步最小动作是读取这两页的原始正文及其中引用的准确版本平台表/Windows样例makefile，不再进行同样的全目录扫描。当前只知道页面存在及其元数据，不能据文件名宣称已核实Visual Studio 2019/2022某版本或链接参数。若页面仅引用另一官方支持表，需要该引用材料才能确定工具集。

next-offline-input-selection.json列了精确相对路径和分组：先两页及Windows样例构建说明，再核对少量生命周期/只读模型体面头；特征树相关头作为后续离线组。无需上传8个巨大.lib、DLL/JDK或全部505个头，也不需要运行编译器/Creo来补文件事实。选取的原始SDK资料仅供后续私人离线审核，不在本报告包重分发。

## 覆盖与身份限制

本次扫描到20000条目边界（entriesSeen=20001，directoriesVisited=152），scanLimited=true、issues为空。达到边界不是错误，也不是全SDK完整性证明；没有某文件记录不能推出不存在。清单过滤规则也可能不收录某些无标准扩展名的样例makefile，因此“未列出实际makefile”不能作为缺文件结论。

根目录只记为<PROTOOLKIT_ROOT>，没有绝对路径；清单未附采集脚本自身散列。需要以补充说明确认该根对应已观察的Creo 13.4安装中的protoolkit目录，不能仅凭目录名/头文件名认证SDK发布版本。库和头只有报告散列，云端没有原件来独立复算，也没有读取函数原型或库ABI。

当前compilerVersion=UNKNOWN_NOT_QUERIED、libraryAbiAndOfficialSupport=UNKNOWN_NOT_VERIFIED、license=UNKNOWN_NOT_COLLECTED、targetReady=false应保持，不能因为13/13头出现就改为ready。

## 后续验证范围保持不变

先完成官方平台/编译器/库变体、合法许可与注册入口契约核查。未来第一版原生受控验证仍只做官方生命周期和模型/单位/唯一默认体/原选择的只读身份检查；不分配业务特征树、不创建、不再生、不保存。之后是否进行内存树和一次创建需另立受控任务，不在当前包自动推进。

当前已有SDK文件基础，不需要因清单曾缺失就采购/覆盖安装。若进一步核对确认某必需组件或合法授权确实欠缺，再按正式安装/采购流程决定；在此之前P1写目标暂停，Java只读价值保留。C++也不是免许可或保证修复原生挂起的途径。

本审核包只包含派生报告、组件元数据和下一步最小资料清单，不包含原始清单ZIP、SDK正文、库二进制、模型或现场日志。
