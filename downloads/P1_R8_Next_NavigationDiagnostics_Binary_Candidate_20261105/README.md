# R8 Next 导航诊断二进制候选

日期：2026-11-05。基于已审核源码候选441ea4bf…，使用用户上传的真实Creo13.4 SDK输入完成编译。本包是独立受控验证候选，不是安装包或正式Release；未部署或运行Creo。

## 已完成

- javac25.0.5.0.3读取真实otk.jar，编译退出码0，stdout/stderr均无输出；没有使用pfcasync.jar或Stub。
- 生成4个class及内容相同的薄JAR；class目标major61（Java17格式），SDK依赖多数major69，运行仍需Java25。
- 在另一个干净目录重复真实编译，4个class散列全部相同；JAR内class与classes/逐字节核对。
- javap静态反汇编核对新增导航诊断；不执行插件、不调用PTC本地库。
- 源码与已审核源码候选完全一致；R8资源9项原字节不变。

classes/包含可供未来受控配置加载的完整4个类；lib/提供薄JAR。source/、patches/、evidence/、manifest.json和SHA256SUMS用于源码到二进制溯源。registration/business-java.dat.template使用独立新classes及资源目录占位符，不能直接注册。

相较R8，唯一业务源码变更为页面导航诊断：保留GetURL和同页跳过SetURL策略，区分pre-navigation探测、导航及激活/刷新失败，保留真实堆栈与requestId，不以API返回宣称页面加载成功。原生转换/其他业务逻辑不变。

相较上一源码候选包，本包新增真实class/JAR、编译和反汇编证据、实际SDK/工具链散列及受控模板；上一包“沿用R8 classes、新日志未生效”的默认说明不适用于新二进制内容。但新日志也只有未来实际加载本候选class后才会产生，尚未真机验证。

详见[二进制审核与受控验证](docs/binary-review-and-controlled-test.md)、[导航日志规则](docs/navigation-events.md)及[只读分析计划](docs/readonly-analysis-plan.md)。SDK/JDK本体不随包分发；reference-only-web继续仅为历史参考，不替换实际Web。

本次没有真机启动、页面点击、模型分析、原生转换、保存、配置恢复或Ribbon变更，也未创建Release/安装包。下一步是本机静态审核，再依单独授权验证新日志。
