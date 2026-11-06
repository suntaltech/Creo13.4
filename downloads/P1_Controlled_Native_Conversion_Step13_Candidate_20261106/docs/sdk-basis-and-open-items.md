# SDK依据与未决项

编译使用用户提供的实际otk.jar，SHA256=0f95b0cf9c88792c0bd18aff4e7a3ef730fdc3d7924ffe681779a40bfcda2319；pfcasync不使用，SDK/JDK不复制入本地候选。仅javac/javap读取，未初始化/执行采集器或SDK模型对象。签名见evidence/sdk-signatures.javap.txt，实际调用点见compiled-bytecode.javap.txt与compiled-sdk-calls.json。

驱动墙元素树依据此前用户SDK输入包内04_驱动面元素树与验收规则.md：根PRO_E_FEATURE_TREE、类型DATUM_SURFACE、名称、PRO_E_DTM_SRF_TYPE=15、PRO_E_SMT_CONV_BODY、PRO_E_SMT_DRV_SURF、PRO_E_STD_SMT_THICKNESS，共7节点。常量15来源是该既有文档，不是根据距离推断；本轮没有独立重新认证完整官方语义或当前几何可接受性。没有加入自动厚度、shell、材料侧/换侧未确认硬编码，也没有STP重新导入。

实际对象若已实现WSolid/WSession直接使用公开接口；否则复用已给出的固定内部适配器构造器：Implementation.wfcPart$WPart(long)、wfcSolid$WSolid(long)、wfcSession$WSession(long)。从实际jxobject_i.getptr取非零指针，检查包装视图类型及指针一致；固有类名固定，不由用户输入任意加载。内部实现不是稳定公开API，反射可能因许可、封装、版本或本机对象失败，签名核对不证明运行安全；失败停止，不以直接未检查强转掩盖不兼容。当前云端不执行这些适配器。

已知原生SDK有GetElementTree、FeatureStatusFlag/GetIsFailed等只读证据接口；它们可用于创建后验证，但不同原生版本/对象/许可及失败树是否可读均待现场。模型变动和错误标志是有限观察，不证明整个会话无写入或未来可转。

新的模型写仅WSolid.WCreateFeature与显式WRegenerate，各一个调用点。参数工厂、UI选项setter、元素树构造用于提交准备；未调用Save、Backup、Copy、Rename、Delete/Erase、Suppress、Undo、Export、RunMacro、精度/单位修改、配置/Ribbon持久化、射线/材料接口。模型默认体/特征树可能被创建/再生改变，不能再称本命令全程只读。

尚未证明：驱动面/材料侧选择是否适合200002100.prt；操作者输入试验厚度是否被原生接受；双对话框/字符串输入实际取消行为；内部WFC视图/许可；再生参数抑制确认的实际作用；回读类型/驱动子类与界面钣金墙对应；失败状态保留与结果显示的现场表现。未验证项不由纯文件控制测试替代，不承诺一次原生提交必然成功。
