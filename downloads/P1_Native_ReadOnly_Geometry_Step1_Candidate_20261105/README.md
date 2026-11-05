# P1 原生只读几何采集：第一步候选

日期：2026-11-05。基于本轮任务单实现独立源码与真实SDK编译产物。未执行Creo、注册应用、GUI操作、模型读取或部署；编译成功不等于真机验证通过。

## 新增能力

独立应用类p1.readonly.geometry.P1ReadOnlyGeometryCollector，独立命令SISWReadOnlyGeometryCollect，列表名称Collect Read-Only Geometry。start只获取同步会话、注册/Designate本命令；只有用户在未来明确授权范围触发该命令后，才同步读取当前测试零件。

采集模型文件名、原生GetRevision字符串、前后修改标志；主长度单位名称/表达式、相对与绝对精度读取值；默认SolidBody的原生IsSheetmetal；通过ITEM_BODY枚举全部返回实体并记录各自状态；通过ITEM_SURFACE枚举模型曲面，记录真实GetSurfaceType代码/名称、正且有限的EvalArea及body/surface ID。实体归属来源为同一模型内Body.ListSurfaces的ID成员关系，不伪装为独立owner查询或默认体归属。

每个读取异常保留实际异常类和堆栈；空值、失败、不支持接口、未知归属、数量/时间限制和未产出记录均明确保留。输出模型/实体/曲面/覆盖率及终结状态，不把原生IsSheetmetal=false等同块状实体。薄壁形态和等厚状态始终INSUFFICIENT_EVIDENCE；点/法向/面间距采样明确NOT_REQUESTED_STEP_1。

## 交付

source/两份源码；classes/六个完整class；lib/p1-readonly-geometry.jar为相同class的薄JAR；registration为带占位符受控模板；resources/text为独立消息文件；evidence含真实编译、重复编译、纯逻辑测试、SDK签名、完整字节码及实际PTC调用清单；manifest和SHA256SUMS用于核验。

真实otk.jar编译使用javac25.0.5.0.3、--release17、UTF-8、-proc:none、-Xlint:all，退出0，无警告。两次干净编译class散列一致，JAR内容与classes一致。目标class为major61，但SDK依赖major69，运行仍需能加载该SDK的Java25；不宣称Java17运行兼容。SDK与JDK不随包分发，pfcasync.jar未使用。

19项纯逻辑检查覆盖JSON、未知/空覆盖率、部分与完成状态等，未使用SDK Stub或模型Mock，没有伪造运行记录；完整字节码常量池核对包含lambda方法引用的30项PTC方法引用，均在本候选清单范围，不含模型修改API。这是静态调用范围检查，不是独立的运行零写入证明。

## 与现有插件关系

不含或替换BatchRename.class，不改变SISWSheetMetalConvert、R8消息资源、现有导航诊断或Web文件。未增加浏览器桥、HTTP、进程启动、后台模型访问、自动Add或Ribbon持久化调用。既有Web转换按钮仍由原纠偏版本保持禁用，本包不修改它。

新应用采用公开Solid/Model/Body/Surface接口并检查运行对象类型，不强转WSolid、不使用内部反射适配。不支持的运行对象或读取接口记录UNKNOWN/PARTIAL；不存在“默认只采一个体却声称所有实体完成”的退化成功路径。公开接口在真实安装环境的行为仍待本机验证。

## 下一步

先按[受控验证说明](docs/controlled-verification.md)核对源码、class/JAR、SDK和资源身份；模板不可原样加载。本轮不改生产dat/config.pro，不修改Ribbon持久化。未来独立授权的会话再注册本应用并触发采集，不点击原生转换。

详见[API与UNKNOWN字段](docs/api-and-unknown-fields.md)及[日志和覆盖率](docs/log-contract.md)。本包不做等厚识别、不承诺模型适合转换，不包含新的转换或保存入口。
