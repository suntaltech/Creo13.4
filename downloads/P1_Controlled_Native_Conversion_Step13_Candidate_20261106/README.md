# P1 第十三步本地受控转换候选

2026-11-06，Asia/Shanghai。本包按后续“打包下载链接”授权生成，包含source、6个独立class、资源/未启用注册模板、SDK签名javap、离线验证与设计文档；上传既有交付分支供下载。未创建插件JAR，未部署或执行Creo，未作生产发布。既有Step10、只读代码和现场配置未修改。

本轮已经实现明确的原生写入口，但真实转换尚未执行，不能声称已在200002100.prt跑通。对象为当前打开的单体普通零件200002100.prt，要求预选一个该体可见平面；驱动面是本实现的显式前提。只接受原生mm单位，厚度由操作者单独输入有限正数，无推荐值/默认值，不读取旧局部距离，不把1.2mm当全局厚度。

独立类p1.controlled.step13.P1ControlledConversionStep13，命令SISWControlledConvertStep13，显示名Controlled Solid to Sheetmetal Step 13。源码见source/P1ControlledConversionStep13.java，状态/持久化控制见source/Step13Control.java。现有BatchRename、Ribbon、只读采集器均未接线；模板仅供审核，不自动注册。

点击先取得本进程及本用户目录持久一次锁；取消/输入错误/前置失败同样消耗一次，不自动重置。锁位于用户目录CreoControlledConversionStep13/step13-200002100-once.jsonl，文件本身是强制flush的JSONL审计。重载/stop/start不删除锁；不得通过删文件、换目录/用户或重启绕过次数限制。锁未成功建立/关键写入失败不提交模型写。若审计之后I/O失败且模型已提交，按失败或未验证状态处理，不补造成功。

两次原生YES/NO对话框都默认NO。第一提示实测原生IsSheetmetal=false、全局壁厚未证明、模型必须可丢弃、失败可能保留部分改变且不保存；确认后才读操作者试验厚度。第二次显示同样风险及准确驱动面ID/试验厚度，明确只创建/再生一次；再次YES且模型上下文重检通过才提交。

采用WFC驱动墙元素树→WSolid.WCreateFeature→一次WRegenerate。没有STP导入、自动Save、模型复制/改名/备份/删除/Undo或自动恢复。失败策略选择任务允许的“明确保留失败状态”：可能已部分改变，持久锁不解除、无下一步/保存路径，错误对话框要求禁止保存/重试；原模型磁盘文件不由本命令覆盖。没有注册全局保存拦截，用户或其他插件入口仍需遵守不得保存的试验要求，不将“命令无Save”称为整个会话硬阻断或独立零写入证明。

真实otk.jar/Java25两次干净编译无警告，6个class散列一致；class major61但依赖SDK major69，运行需Java25。纯控制35项断言（不带SDK classpath）覆盖双确认顺序、无默认厚度、持久一次锁、关闭/失败不解锁及并发仅一个文件创建成功。并发仅用于纯文件锁测试，产品不创建线程、不后台调用原生API。实际字节码87项PTC方法引用，单个WCreateFeature调用点、单个显式WRegenerate调用点；没有Save/导出/删除/宏/配置/射线/材料调用。不是沿用只读58项调用面，本类包含受控模型写操作。

原生对话框、WFC内部适配、单次转换、失败保留、特征树/状态回读和视觉树均NOT_EXECUTED/UNKNOWN。候选包可审核，不是生产可发布版本；SDK签名存在不证明许可/参数语义/当前几何可接受。材料夹层、遮挡、最终等厚和可转换生产性仍未认证。

详见docs/control-flow-and-failure.md、docs/sdk-basis-and-open-items.md、docs/single-click-observation.md与evidence/offline-validation.json。单次原生调用代码已存在，不能为了测试前置条件而随意点击或绕过锁。
